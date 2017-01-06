(defn ver [] (-> "hare.version" slurp .trim))

(defproject hare (ver)
  :description "Microservice RPC Library using RabbitMQ as transport"
  :url "https://github.com/BrianEdwards85/hare"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [com.novemberain/langohr "3.7.0"]
                 [clj-time "0.12.2"]]
  :plugins [[lein-midje "3.2.1"]]
  :profiles {:dev {:dependencies [[midje "1.8.3"]]}})
