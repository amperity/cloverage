(ns leiningen.cloverage
  (:require [leiningen.core.eval :as eval]
            [leiningen.core.main :as main])
  (:import (clojure.lang ExceptionInfo)))

(defn get-lib-version
  [project]
  (or (System/getenv "CLOVERAGE_VERSION")
      ;; Use the same version of cloverage as lein-cloverage.
      (->> project
           (:plugins)
           (filter (comp #{'amperity/lein-cloverage} first))
           (first)
           (second))))

(defn already-has-cloverage? [project]
  (seq (for [[id _version] (:dependencies project)
             :when (#{'amperity/cloverage 'cloverage/cloverage} id)]
         (do (when (= 'cloverage/cloverage id)
               (println "WARN: non-Amperity cloverage is on the classpath"))
             true))))

(defn ^:pass-through-help cloverage
  "Run code coverage on the project.

  You can set the CLOVERAGE_VERSION environment variable to override what
  version of cloverage is used, but it's better to set it in :dependencies.

  Specify -o OUTPUTDIR for output directory, for other options run
  `lein cloverage --help`."
  [project & args]
  (let [project (if (already-has-cloverage? project)
                  project
                  (update-in project [:dependencies]
                             conj    ['amperity/cloverage (get-lib-version project)]))
        opts    (assoc (:cloverage project)
                       :src-ns-path (vec (:source-paths project))
                       :test-ns-path (vec (:test-paths project)))]
    (try
      (eval/eval-in-project project
                            `(cloverage.coverage/run-project '~opts ~@args)
                            '(require 'cloverage.coverage))
      (catch ExceptionInfo e
        (main/exit (:exit-code (ex-data e) 1))))))
