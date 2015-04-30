(ns snom-phonebook.ldap
  (:require [clojure.pprint :refer [pprint]]
            [clojure.set :refer [rename-keys]])
  (:import java.util.Hashtable
           [javax.naming AuthenticationException Context]
           [javax.naming.directory SearchControls BasicAttributes]
           [javax.naming.ldap InitialLdapContext Rdn LdapName]))


(defn ^:private get-from-attributes [attributes key]
  "Given javax.naming.directory.Attributes 'attributes' and 'key',
  this function searches for the values with the key 'key' and returns
  the first of this values."
  (some-> attributes
          (.get key)
          .getAll
          enumeration-seq
          first))

(defn ^:private search-result->map
  "If all of the given keys have matching values in the search-result,
  this function returns a map with the given keys associated with the
  values of the search-result (for each key the value is the first
  entry of found values for key).  If one key hasn't got a matching
  value in the search-result, this function returns nil."
  [search-result keys]
  (let [attributes (and search-result (.getAttributes search-result))]
    (reduce
     (fn [result-map key]
       (let [value (get-from-attributes attributes key)]
         (if value
           (assoc result-map key value)
           (reduced nil))))
     {}
     keys)))

(defn search
  "Searches a LDA given by an LDAP-configuration 'configuration-map'
  for entries having a cn starting with 'needle' and at least one
  telephoneNumber. Returns a map with the found cns as keys and the
  first found telephoneNumber of each entry as values."
  [configuration-map needle]
  (let [{ldap-server :server
         search-base :basedn} configuration-map
         search-controls (SearchControls.)
         environment (Hashtable.
                      {Context/SECURITY_AUTHENTICATION "simple"
                       Context/INITIAL_CONTEXT_FACTORY "com.sun.jndi.ldap.LdapCtxFactory"
                       Context/PROVIDER_URL ldap-server
                       ;; "com.sun.jndi.ldap.trace.ber" System/err
                       })
         context (InitialLdapContext. environment nil)]
    (.setSearchScope search-controls SearchControls/SUBTREE_SCOPE)
    (let [results (.search
                   context
                   search-base
                   (str "(&" "(cn=" needle "*" ")" "(telephoneNumber=*))")
                   search-controls)
          results (and results (enumeration-seq results))]
      ;; reduce the objects which were returned from java/ldap to a
      ;; map whitch maps names (previous: cn) to telephonenumbers
      (reduce
       (fn [results-map search-result]
         (let [search-result-map (search-result->map
                                  search-result
                                  ["cn" "telephonenumber"])
               cn (get search-result-map "cn")
               telephonenumber (get search-result-map "telephonenumber")]
           (assoc results-map cn telephonenumber)))
       {}
       results))))
