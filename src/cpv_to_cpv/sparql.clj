(ns cpv-to-cpv.sparql
  (:import [com.hp.hpl.jena.sparql.core DatasetImpl] 
           [com.hp.hpl.jena.query Query QueryExecutionFactory QueryFactory QuerySolution]))

; ----- Private functions -----

(defn- process-select-binding
  "Process SPARQL select binding of @var-name from @solution into string"
  [^QuerySolution solution
   ^String var-name]
  (.toString (.get solution var-name)))

(defn- process-select-solution
  "Process SPARQL SELECT @solution."
  [^QuerySolution solution]
  (map (partial process-select-binding solution)
       (iterator-seq (.varNames solution))))

; ----- Public functions -----

(defn select-query
  "Execute SPARQL SELECT query from @query-string on TDB @dataset."
  [^DatasetImpl dataset query-string]
  (let [query (QueryFactory/create query-string)
        qexec (QueryExecutionFactory/create query dataset)]
    (try
      (let [results (.execSelect qexec)
            variables (.getResultVars results)]
        (doall (map process-select-solution (iterator-seq results))))
      (finally (.close qexec)))))
