(ns cpv-to-cpv.link
  (:import [com.hp.hpl.jena.tdb TDBFactory]
           [com.hp.hpl.jena.sparql.core DatasetImpl]
           [org.apache.jena.riot RDFDataMgr])
  (:require [taoensso.timbre :as timbre]
            [clojure.java.io :refer [resource writer]]
            [cpv-to-cpv.sparql :refer [select-query]]
            [clojure.math.combinatorics :refer [combinations]]
            [clj-fuzzy.metrics :refer [jaro-winkler]]))

(declare exact-match? normalize-labels)

; ----- Private vars -----

(def ^:private tdb-directory "db")

; ----- Records -----

(defrecord CPVConcept [uri labels])

(defrecord CPVLabels [bg cs da de el en es et fi fr ga hu it lv lt mt nl pl pt ro sk sl sv])

; ----- Private functions -----

(def ^:private distinct-set?
  "Predicate to determine if 2 CPV concepts are not siblings or
  in skos:broaderTransitive relation."
  (let [cpv-code-pattern (re-pattern #"\d{8}$")
        trailing-zeros (re-pattern #"0{0,6}$")
        last-digit (re-pattern #"\d$")
        rtrim-zeros #(clojure.string/replace (re-find cpv-code-pattern %) trailing-zeros "")
        siblings? (fn [[a b]]
                    (let [a-length (count a)
                          b-length (count b)]
                      (when (and (= a-length b-length) (> a-length 2))
                        (apply = (map #(clojure.string/replace % last-digit "") [a b])))))
        contained? (fn [[a b]] (or (not= -1 (.indexOf a b))
                                   (not= -1 (.indexOf b a))))]
    (comp (every-pred (complement contained?) (complement siblings?))
          (partial map (comp rtrim-zeros :uri)))))

(defn- compute-cpv-similarity
  "Compute similarity between CPV concept @a and @b."
  [[a b]]
  (let [labels-a (normalize-labels a)
        labels-b (normalize-labels b)
        similarity-score (if (exact-match? labels-a labels-b)
                           1
                           (->> (map jaro-winkler 
                                     labels-a
                                     labels-b)
                                (sort >)
                                (take 3)
                                (apply +)
                                (* 1/3)))]
    {:score similarity-score
     :a (:uri a)
     :b (:uri b)}))

(defn- exact-match?
  "Is there an exactly same label in @labels-a and @labels-b?"
  [labels-a labels-b]
  (some (partial apply =) (map vector labels-a labels-b)))

(defn- init-dataset
  "Creates TDB dataset and loads an RDF file on @file-path into it.
   Returns the dataset."
  [^String file-path]
  (let [dataset (TDBFactory/createDataset tdb-directory)]
    (when (.isEmpty (.getDefaultModel dataset))
      (timbre/debug "Loading data...")
      (RDFDataMgr/read dataset file-path))
    dataset))

(defn- load-cpv-labels
  "Load labels of CPV codes from @input."
  [input]
  (let [dataset (init-dataset input)
        query-string (slurp (resource "get_cpv_labels.rq"))
        transform-labels (fn [labels]
                           (into {} (map (juxt (comp keyword last) first) labels)))
        transform (fn [[cpv labels]]
                    (CPVConcept. cpv (map->CPVLabels (transform-labels labels))))]
    (doall (->> (select-query dataset query-string)
                (group-by second)
                (map transform)))))

(defn- match->ntriples
  "Create NTriples `skos:related` link between @a and @b."
  [{:keys [a b]}]
  (format "<%s> <http://www.w3.org/2004/02/skos/core#related> <%s> . \n" a b))

(defn- normalize-labels
  "Normalize CPV labels of concept @cpv.
  So far does lowercasing only."
  [cpv]
  (map (comp clojure.string/lower-case second)
       (sort-by first (:labels cpv))))

; ----- Public functions -----

(defn find-links
  "Find associative links in CPV input, for which the computed similarity
  is above the @threshold. Export results into NTriples file @output."
  [{:keys [input output threshold]}]
  (let [log-fn (fn [index cpvs]
                 (when (and (not (zero? index)) (zero? (mod index 100000)))
                    (timbre/debug (format "%s comparisons done." index)))
                 cpvs)
        cpv-labels (load-cpv-labels input)
        cpv-pairs (combinations cpv-labels 2)]
    (println "Looking hard for links...")
    (with-open [output-file (writer output)]
      (dorun (->> cpv-pairs
                  ;(take 10000)
                  (filter distinct-set?)
                  (map-indexed log-fn)
                  (pmap compute-cpv-similarity)
                  (filter (comp (partial <= threshold) :score))
                  (map match->ntriples)
                  (map #(.write output-file %)))))))
