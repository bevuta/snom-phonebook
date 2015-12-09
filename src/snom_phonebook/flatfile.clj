(ns snom-phonebook.flatfile
  (:require [clojure.java.io :as io]
            [clojure.string :as s]))

(defn ^:private extract-name-and-number-map
  "This function extracts parts of the given 'line' with a 'regular-expression'.
  With 'name-field' and 'number-field' (both integers) you can specify
  which extracted part is the name and which is the
  number. 'max-field' is just the maximum of 'name-field' and
  'number-field'."
  [regular-expression needle-pattern name-field number-field max-field line]
  (let [matches (rest (re-find regular-expression line))]
    (when (and (not (empty? matches))
               (>= (count matches) max-field)
               (some #(re-find needle-pattern %)
                     (s/split (nth matches name-field) #" ")))
      {(nth matches name-field)
       (nth matches number-field)})))

(defn search
  "Given a 'configuration-map' for flatfiles and a needle, this
  functions searches a file for name/number pairs where the names were
  matched by 'needle' and returns the pairs as a map.
  'configuration-map' is a map with a ':file' name
  (inclusive path), a ':regular-expression' which should extract (at
  least) the name and number from every line in ':file', a
  'name-field', which is an integer and describes the 'name-field'th
  extracted field and similar for 'number-field' as for 'name-field'."
  [configuration-map needle anchor?]
  (let [{:keys [regular-expression
                name-field
                number-field]} configuration-map
        needle-pattern (re-pattern (str (if anchor? "(?i)^" "(?i)") needle))
        extractor-fn (partial extract-name-and-number-map
                              regular-expression
                              needle-pattern
                              name-field
                              number-field
                              (max name-field number-field))]
    (with-open [reader (io/reader (:file configuration-map))]
      (reduce merge
              (map extractor-fn
                   (line-seq reader))))))
