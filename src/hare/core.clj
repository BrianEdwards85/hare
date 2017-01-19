(ns hare.core
  (:gen-class)
  (:require [langohr.core       :as rmq]
            [langohr.channel    :as lch]
            [langohr.queue      :as lq]
            [langohr.exchange   :as le]
            [langohr.consumers  :as lc]
            [langohr.basic      :as lb]
            [manifold.deferred :as d]))

;;Connection Map{
;; :rabbit {....}  RabbitMQ Connection Paramaters
;; :reply-queue-name queue to reply
;; :sub-ex         Exchange to subscribe
;; :pub-ex         Exchange to publish to
;;}

(defn extra-paramater-map [v]
  (cond
    (or  (nil? v) (empty? v)) {}
    (map? v) v
    (not (sequential? v)) (throw (IllegalArgumentException. "Sequence expected"))
    (= (count v) 1) (if (map? (first v)) (first v) (throw (IllegalArgumentException. "Map expected" )))
    (even? (count v)) (apply hash-map v)
    :else (throw (IllegalArgumentException. "Unable to create map" ))
    ))

(defn get-reply-handler [replies]
  (fn handle-reply [ch meta payload]
    (let [reply-id (:correlation-id meta)]
      (when-let [p (get @replies reply-id)]
        (do
          (d/success! (:p p) payload)
          (swap! replies #(dissoc % reply-id)))))))

(defn connect [{:keys [rabbit reply-queue-name sub-ex pub-ex] :as parameters}]
  (let [conn (rmq/connect rabbit)
        ch (lch/open conn)
        replies (atom {})
        reply-queue (lq/declare ch reply-queue-name {:exclusive true :auto-delete true})]
    (do (lq/bind ch reply-queue-name sub-ex {:routing-key reply-queue-name})
        (lc/subscribe ch reply-queue-name (get-reply-handler replies) {:auto-ack true})
        (merge parameters 
               {:conn conn
                :queues (atom {})
                :reply-queue {:name reply-queue-name :q reply-queue :ch ch}
                :replies replies}))))

(defn get-queue [{:keys [conn queues sub-ex] :as parameters} queue]
  (if-let [q (get @queues queue)] q
          (let [ch (lch/open conn)
                q (lq/declare ch queue {:exclusive false :auto-delete true})
                qmap {:queue  q :ch ch}]
            (do
              (lq/bind ch queue sub-ex {:routing-key queue})
              (swap! queues #(assoc % queue qmap)))
            qmap)))

(defn subscribe-to [parameters queue f]
  (let [qmap (get-queue parameters queue)
        ch (:ch qmap)]
    (lc/subscribe ch queue f {:auto-ack true})))

(defn send-message
  [{:keys [conn pub-ex] :as parameters} k payload & options]
  (let [ch (lch/open conn)
        option-map (extra-paramater-map options)]
    (do
      (lb/publish ch pub-ex k payload option-map)
      (rmq/close ch))))

(defn reply-promise [{:keys [replies] :as parameters} i]
;;  (let [p (promise)]
  (let [p (d/deferred)]
    (swap! replies  #(assoc % i {:p p :id i :exp nil }))
    p))

(defn send-with-reply [paramaters k payload]
  (let [message-id (str (java.util.UUID/randomUUID))
        reply (reply-promise paramaters message-id)]
    (send-message paramaters k payload {:message-id message-id
                             :reply-to (get-in paramaters [:reply-queue :name] )})
    reply))

(defn send-reply [paramaters ch {:keys [reply-to message-id] :as  meta} payload & options]
  (let [reply-message-id (str (java.util.UUID/randomUUID))
        option-map (extra-paramater-map options)
        publish-options (merge option-map {:message-id reply-message-id :correlation-id message-id})]
    (lb/publish ch (:pub-ex paramaters) reply-to payload publish-options)))

;;        reply-queue-name (format "reply.user.%s" instance-id)
;;(def ex-name "command.routing")
;;(defonce instance-id (str (java.util.UUID/randomUUID)))
;;(def connection-parameters  { :username "bedwards" :password "UNH2qgYN" :host "edwardstx.us" :port 5672 :vhost "dev"})
;; (defn connect [] (rmq/connect connection-parameters))
;;(defonce state (atom {}))

;;(defn get-channel
;;  ([] (get-channel (:conn @state )))
;;  ([conn] (lch/open conn)))
