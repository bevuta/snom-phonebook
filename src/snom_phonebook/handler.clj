(ns snom-phonebook.handler
  (:require [clojure.data.xml :refer [element
                                      emit-str
                                      indent-str
                                      sexp-as-element]]
            [clojure.pprint :refer [pprint]]
            [clojure.string :as s]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [immutant.web :as http-server]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [snom-phonebook.ldap :as ldap]
            [snom-phonebook.flatfile :as flatfile]
            [snom-phonebook.mysql :as mysql]
            [snom-phonebook.strings :as strings])
  (:gen-class))

(def ^:private configuration-file "configuration.clj")

(def ^:private configuration (read-string (slurp configuration-file)))

(defn ^:private ->menu-item [name url]
  [:MenuItem {}
   [:Name {} name]
   [:URL {} url]])

(def ^:private menu-items
  (let [base-url (:base-url configuration)]
   (for [[range url-part] [[strings/search "search"]
                           ["A-C" "get/A-C"]
                           ["D-F" "get/D-F"]
                           ["G-I" "get/G-I"]
                           ["J-L" "get/J-L"]
                           ["M-O" "get/M-O"]
                           ["P-S" "get/P-S"]
                           ["T-V" "get/T-V"]
                           ["W-Z" "get/W-Z"]
                           ["0-9" "get/0-9"]
                           ["ÄÖÜ" "get/umlauts"]
                           [strings/help "help"]]]
     [range (str base-url url-part)])))

(defn ^:private ->snom-ip-phone-directory
  [title prompt directory-entries]
  (sexp-as-element
   [:SnomIPPhoneDirectory {}
    [:Title {} title]
    [:Prompt {} prompt]
    (for [directory-entry directory-entries]
      [:DirectoryEntry {}
       [:Name {}
        (first directory-entry)]
       [:Telephone {}
        (last directory-entry)]])]))

(defn ^:private ->snom-ip-phone-menu [title menu-items]
  (sexp-as-element
   [:SnomIPPhoneMenu {}
    [:Title {} title]
    (map #(->menu-item (first %) (last %)) menu-items)]))

(defn ^:private ->snom-ip-phone-text [title text]
  (sexp-as-element
   [:SnomIPPhoneText {}
    [:Title {} title]
    [:Prompt {} ""]
    [:Text {} text]]))

(defn ^:private snom-ip-phone-input
  ([url query-string-parameter title display-name]
   (sexp-as-element
    [:SnomIPPhoneInput
     [:Title title]
     [:Prompt]
     [:URL url]
     [:InputItem
      [:DisplayName display-name]
      [:QueryStringParam query-string-parameter]
      [:DefaultValue]
      [:InputFlags "a"]]])))

(defn ^:private prefix-phonenumber
  "If 'phonenumber' starts with a plus, the result is
  00'prefix''phonenumber', if not, the result is
  'prefix''phonenumber'. Leading open parentheses '(' will be ignored"
  [phonenumber prefix]
  (let [s (drop-while (comp #{ \( }) (seq phonenumber))]
    (str prefix (if (= \+ (first s))
                  (apply str (cons "00" (rest s)))
                  (apply str s)))))

(defn ^:private search [query]
  "Searches the string 'query' in each configured handle. Returns
  things like this:
  [[name number] [name2 number-for-name2]]"
  (let [handlers (:handlers configuration)]
    (->> handlers
      (mapcat (fn [handler]
                (let [type (:type handler)
                      results (case type
                                :ldap (ldap/search handler query)
                                :mysql (mysql/search handler query)
                                :flatfile (flatfile/search handler query)
                                {})
                      prefix (:prefix handler)]
                  (if prefix
                    (for [[k v] results]
                      [k (prefix-phonenumber v prefix)])
                    results))))
      vec)))

(defn ^:private multisearch
  "Acts like search, with the difference that you can pass multiple
  query-strings as vector and the results will be merged into one. The
  result looks like the result of 'search'."
  [queries]
  (->> queries
    (map #(search %))
    (reduce concat)))

(defn ^:private strip-non-numeric
  "Kicks every nom-number-character out of 'phonenumber' and then
  replaces leading 000'countryprefix' with only 2 leading 0s."
  [phonenumber countryprefix]
  (let [stripped-number (s/replace phonenumber #"[^0-9]" "")]
    (s/replace
     stripped-number
     (re-pattern (str "^000" countryprefix))
     "00")))

(defn ^:private wrap-search-results
  "Returns xml for 'results' after calling 'strip-non-numeric' on each
  result with 'countryprefix' and subsequent sorting of all the
  results by name."
  [countryprefix results]
  (if (empty? results)
    (emit-str (->snom-ip-phone-menu strings/phonebook (list [strings/no-results-back (:base-url configuration)])))
    (->> results
      (map (fn [[name phonenumber]]
             [name (strip-non-numeric
                    phonenumber
                    countryprefix)]))
      (into {})
      vec
      (sort-by first)
      (->snom-ip-phone-directory strings/phonebook "")
      indent-str)))

(def ^:private app-routes
  (let [country-prefix (:country-prefix configuration)
        multi-search-partial (partial #(wrap-search-results country-prefix (multisearch %)))]
    (routes
     (GET "/" [] (indent-str (->snom-ip-phone-menu strings/phonebook menu-items)))
     (context
      "/get" []
      (GET "/A-C" [] (multi-search-partial ["^A" "^B" "^C"]))
      (GET "/D-F" [] (multi-search-partial ["^D" "^E" "^F"]))
      (GET "/G-I" [] (multi-search-partial ["^G" "^H" "^I"]))
      (GET "/J-L" [] (multi-search-partial ["^J" "^K" "^L"]))
      (GET "/M-O" [] (multi-search-partial ["^M" "^N" "^O"]))
      (GET "/P-S" [] (multi-search-partial ["^P" "^Q" "^R" "^S"]))
      (GET "/T-V" [] (multi-search-partial ["^T" "^U" "^V"]))
      (GET "/W-Z" [] (multi-search-partial ["^W" "^X" "^Y" "^Z"]))
      (GET "/0-9" [] (multi-search-partial ["^0" "^1" "^2" "^3" "^4" "^5" "^6" "^7" "^8" "^9"]))
      (GET "/umlauts" [] (multi-search-partial ["^Ä" "^Ö" "^Ü" "^ä" "^ö" "^ü"])))
     (GET "/search" request
          (let [query-params (:query-params request)]
            (if (empty? query-params)
              (emit-str (snom-ip-phone-input (str (:base-url configuration) "search") "search" strings/search strings/search))
              (when-let [query (get query-params "search")]
                (let [query (s/replace query #"[*]" (s/re-quote-replacement (str "\\" "*")))
                      results (search query)]
                  (wrap-search-results country-prefix results))))))
     (GET "/help" [] (emit-str
                      (->snom-ip-phone-text
                       strings/phonebook
                       strings/help-string)))
     (route/not-found strings/not-found))))

(defn -main
  "Starts a http-server on the configured port."
  [& args]
  (print (str "Server started on host " (:host configuration)
              " and port " (:port configuration)))
  (http-server/run
    (wrap-defaults app-routes site-defaults)
    :host (:host configuration)
    :port (:port configuration)))
