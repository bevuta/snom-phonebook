(ns snom-phonebook.mysql
  (:require [clojure.java.jdbc :as jdbc]))

(defn ^:private format-result
  "Takes a map 'query-result', a string 'phone-element' and a reversed
  format sequence ('reverse-format'). Returns a map with the formatted
  name as the only key and the 'phone-element' value of query-result
  as it's value. This function is meant as a helper function for
  format-results."
  [query-result phone-element reverse-format]
  ;; note: successor-exists indicates that a format-part starting with
  ;; '>' should (or shouldn't) be a part of the resulting "name"
  {(-> (reduce
        (fn [{:keys [result successor-exists] :as accu}
             format-part]
          (case (first format-part)
            ;; a format-part starting with ! means, that the
            ;; format-part is included in the resulting string
            ;; anyways. :successor-exists will be reset to false.
            \! {:result (str (subs format-part 1) result)
                :successor-exists false}
            ;; Only if the successor exists, display this format-part in
            ;; the resulting "name".
            \> (if successor-exists
                 {:result (str (subs format-part 1) result)
                  :successor-exists successor-exists}
                 accu)
            ;; nothing special formatting here, but when the part is
            ;; found in the query-result, make it part of the
            ;; resulting "name". :successor-exists will be set to true
            ;; anyways (according to the old python-script behaviour).
            {:result (str ((keyword format-part) query-result) result)
             :successor-exists true}))
        {:result ""
         :successor-exists false}
        reverse-format)
     (:result))
   (phone-element query-result)})

(defn ^:private format-results
  "Takes a sequence of maps (here named 'results'), a string
  'phone-element' and a 'format' sequence (see example configuration).
  Returns _one_ map where the keys are the formatted names and the
  values are the phone-number specified by 'phone-element'. This
  function is meant as a helper function for search."
  [format phone-element results]
  (let [reverse-format (reverse format)
        phone-element (keyword phone-element)]
    (apply
     merge
     (map
      #(format-result % phone-element reverse-format)
      results))))

(defn ^:private query-database
  "Queries the table 'table' in database given by
  'database-configuration' for all rows having at least one column of
  'search-elements' starting with 'query' (case-insensitive) and a
  non-empty 'phone-element' column.  Returns a sequence of these rows
  where each row is a map with keys consisting of 'name-elements' and
  'phone-element', e.g. {:name-element1 value :name-element2
  value2 :phone-element value3}. This function is meant as a helper
  function for search."
  [database-configuration name-elements phone-element table search-elements query]
  (jdbc/query
   database-configuration
   (into
    [(str
      "SELECT " (apply str (interpose "," name-elements)) "," phone-element " FROM "
      "(SELECT * FROM " table " "
      "WHERE (" (apply
                 str
                 (interpose " OR "
                            (map #(str % " LIKE ?")
                                 search-elements))) ")"
      " AND (" phone-element " IS NOT NULL)"
      " AND (" phone-element " != '')"
      ") AS tmp")]
    (map (constantly query) search-elements))))

(defn search
  "Given a 'configuration-map' consisting of :database-configuration,
  :table, :name-elements, :search-elements, :phone-element, :format
  and a 'query'-string, this function returns a name/phone-number map
  for the results which were found.  For an example of
  'configuration-map', see the example-configuration."
  [configuration-map query anchor?]
  (let [{:keys [database-configuration
                table
                name-elements
                search-elements
                phone-element
                format]} configuration-map
                query (str (if anchor? "" "%") query "%")]
    (format-results
     format
     phone-element
     (query-database database-configuration
                     name-elements
                     phone-element
                     table
                     search-elements
                     query))))
