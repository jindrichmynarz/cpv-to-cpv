(ns cpv-to-cpv.core
  (:gen-class)
  (:require [taoensso.timbre :as timbre]
            [clojure.string :refer [join]]
            [clojure.tools.cli :refer [parse-opts]]
            [clojure.java.io :refer [as-file]]
            [cpv-to-cpv.link :refer [find-links]]))

; ----- Private vars -----

(def ^:private
  cli-options
  [["-i" "--input RDF" "Path to input RDF file"
    :validate [#(.exists (as-file %)) "The input RDF file must exist"]]
   ["-o" "--output RDF" "Path to output RDF file"]
   ["-t" "--threshold NUMBER" "Similarity threshold"
    :default 0.95
    :parse-fn #(Double/parseDouble %)
    :validate [pos? (partial > 1)]]
   ["-h" "--help"]])

; ----- Private functions -----

(defn- error-msg
  [errors]
  (str "The following errors occurred while parsing your command:\n\n"
       (join \newline errors)))

(defn- exit
  "Exit with @status and message @msg"
  [^Integer status
   ^String msg]
  (println msg)
  (System/exit status))

(defn- init-logger
  "Initialize logger"
  []
  (let [logfile "log/cpv-to-cpv.log"
        log-directory (as-file "log")]
    (when-not (.exists log-directory) (.mkdir log-directory))
    (timbre/set-config! [:appenders :standard-out :enabled?] false)
    (timbre/set-config! [:appenders :spit :enabled?] true)
    (timbre/set-config! [:shared-appender-config :spit-filename] logfile)))

(defn- usage
  [options-summary]
  (->> ["CPV associative link discovery tool"
        ""
        "Usage: cpv-to-cpv [options]"
        ""
        "Options:"
        options-summary]
       (join \newline)))

; ----- Public functions -----

(defn -main
  [& args]
  (let [{{:keys [help]
          :as options} :options
         :keys [errors summary]} (parse-opts args cli-options)]
    (cond (or (empty? options) help) (exit 0 (usage summary))
          errors (println (exit 1 (error-msg errors)))
          :else (do (init-logger)
                    (find-links options)
                    (shutdown-agents)))))
