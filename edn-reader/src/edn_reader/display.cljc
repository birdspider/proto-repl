(ns edn-reader.display
    "Contains functions for converting EDN data structures into trees for display
     in Atom Ink"
    (:require [clojure.string :as str]
              [clojure.set :as set]))

(defn to-display-tree*
  "Converts a value into a displayable tree. "
  [v]
  (cond
    ;; Handles a map.
    (map? v)
    (into [(pr-str v)]
       ;; Loop over each map entry
       (map (fn [entry]
              [(pr-str entry)
               (to-display-tree* (second entry))])
            v))

    ;; Handles a sequence
    (or (sequential? v) (set? v))
    (into [(pr-str v)] (map to-display-tree* v))

    ;; Leaf
    :else [(pr-str v)]))


(def max-table-width
  "Sets the maximum width of a table in characters when "
  60)

(def ellipsis
  "A single character ellipsis for truncating long values.
   Unicode: U+2026, UTF-8: E2 80 A6"
  "…")

(def min-column-width
  "The minimum size a column could be shrunk to is 1. That assumes 1 space for
   data and 1 for an ellipsis. 'X…'"
  2)


(defn value-map->printable-map
  "Takes a map of var names to their values and returns the map with values
   in a string format."
  [vm]
  (into {} (for [[k v] vm] [k (pr-str v)])))

(defn common-keys
  "Find the common set of keys in all of the maps"
  [maps]
  (reduce (fn [common-keys m]
            (set/union common-keys (set (keys m))))
          #{}
          maps))

(defn max-value-widths
  "Returns a map of the max lengths of all the given keys in the maps. Assumes
   values in maps are strings."
  [keys value-maps]
  (reduce (fn [max-width-map vm]
            (into {} (for [[k max-width] max-width-map
                           :let [width (count (get vm k ""))]]
                       [k (max max-width width)])))
          ;; initial max widths are 0
          (zipmap keys (repeat 0))
          value-maps))

(comment
 (max-value-widths
  [:a :b]
  [{:a "123" :b "1"}
   {:a "12" :b "1234"}]))

(defn exception
  "Creates an exception with the given message"
  [message]
  #?(:clj (Exception. message)
     :cljs (js/Error. message)))

(defn col-widths->table-width
  "Returns the size of the table based on a map of column to sizes"
  [widths]
  (let [num-cols (count widths)]
   ;; space for all the columns ...
   (+ (apply + (vals widths))
      ;; + " |" after each column
      (* 2 num-cols)
      ;; + space after pipe except on the last column
      (dec num-cols))))

(defn calculate-columns-widths
  "Takes a map of widths for each column and returns a new map of widths such
   that the values will fit within the max-table-width. Assumes that the number
   of columns is possible to fit within the table."
  [max-widths]
  (let [num-cols (count max-widths)
        ;; Calculate the width of the table if nothing was shrunk
        table-width (col-widths->table-width max-widths)
        ;; This is the total amount we need to remove.
        amount-to-shrink (- table-width max-table-width)]
    (if (<= amount-to-shrink 0)
      ;; No shrinking required
      max-widths
      ;; Select out all the columns that can be shrunk
      (let [shrinkable-cols (for [[k width] max-widths
                                  :when (> width min-column-width)]
                              [k width])]
        ;; Iterate through the shrinkable columns starting with the largest
        (loop [shrinkable-cols (reverse (sort-by second shrinkable-cols))
               ;; Keep track of the current set of width values, amount left to shrink
               widths max-widths
               amount-to-shrink amount-to-shrink
               num-recursions 0]
          ;; Guard against infinite loops due to logic problems
          (when (> num-recursions num-cols)
            (throw (exception (str "Number of recursions [" num-recursions
                                   "] exceeded while calculating column widths"))))

          (let [shrinkable-col-size (apply + (map second shrinkable-cols))
                [col-to-shrink size] (first shrinkable-cols)
                ;; Calculate the percentage to shrink based on the total width
                ;; of columns that can be shrunk
                col-amt-to-shrink (-> (/ size (double shrinkable-col-size))
                                      (* amount-to-shrink)
                                      Math/ceil
                                      long)
                ;; Shrink the column
                new-widths (assoc widths col-to-shrink (- size col-amt-to-shrink))]
            (if-let [other-cols-to-shrink (seq (rest shrinkable-cols))]
              ;; If there are more columns to shrink keep recursing.
              (recur other-cols-to-shrink
                     new-widths
                     (- amount-to-shrink col-amt-to-shrink)
                     (inc num-recursions))
              ;; We've shrunken all the columns proportionally.
              new-widths)))))))


(comment
 (calculate-columns-widths
  {:a 70 :b 70 :c 70}))

(defn fit-value-to-width
  "Takes a width and a string value and returns the string so that it exactly
   fits the width given. The value is truncated if it is too long with an
   ellipsis or has spaces prepended."
  [width value]
  (let [length (count value)]
    (if (> length width)
      (str (subs value 0 (dec width)) ellipsis)
      (str (str/join (repeat (- width length) " ")) value))))

(defn row->str
  "Takes a list of keys ordered for the row, a map of values for the row, and
   the available space for each column and returns a string row with columns
   separated by a pipe character."
  [key-order value-map col-widths]
  (let [value-strs (for [k key-order]
                     (fit-value-to-width (col-widths k) (get value-map k)))]
    (str (str/join " | " value-strs) " |")))

(defn- value-maps->table-rows
  "Takes a set of maps containing variable names and values and returns a set
   of string rows that will fit within the max-table-width."
  [value-maps]
  (let [printable-maps (map value-map->printable-map value-maps)
        keys (common-keys printable-maps)
        key-printable-map (zipmap keys (map pr-str keys))
        printable-maps (cons key-printable-map printable-maps)
        ;; TODO bail out if min table width is > max table width
        max-widths (max-value-widths keys printable-maps)
        col-widths (calculate-columns-widths max-widths)
        key-order (map first (sort-by second col-widths))]
    (map #(row->str key-order % col-widths) printable-maps)))

(comment
 (println "----")
 (doseq [row (value-maps->table-rows
              [{:a [1 2 3 4 5 6 7] :b [1 2 3 4 5 6 7 8] :c 2 :d 5444}
               {:a [1 2 3 4 5 6 7] :b [1 2 3 4 5 6 7 8]}])]
   (println row)))

(defn- value-map->display-tree-values
  "Takes a map of variable names and values and converts it into a displayable
   tree of values."
  [value-map]
  (for [[var-name value] value-map
        :let [val-display-tree (to-display-tree* value)]]
    (update-in val-display-tree [0] #(str var-name ": " %))))

(defn saved-value-maps->display-tree-table
  "Takes a list of maps of variable names to values and converts it into a table
   of each map showing the values. Each row can be expanded to show more details
   of the values in the event any of them had to be truncated."
  [value-maps]
  (let [[header & rows] (value-maps->table-rows value-maps)]
    ;; Indent header by two spaces
    (cons (str "  " header)
          (map (fn [row vm]
                 (cons row (value-map->display-tree-values vm)))
               rows
               value-maps))))