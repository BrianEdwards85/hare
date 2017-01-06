(ns hare.core-test
  (:require [midje.sweet :refer :all]
            [hare.core-data :refer :all]
            [hare.core :refer :all]))

(facts "About extra-paramater-map"
       (fact "Empty map"
             (extra-paramater-map nil) => {}
             (extra-paramater-map []) => {})
       (fact "Passed map"
             (extra-paramater-map [reply-map]) => reply-map
             (extra-paramater-map reply-map) => reply-map
             (extra-paramater-map :else) => (throws IllegalArgumentException))
       (fact "Passed map values"
             (extra-paramater-map [:a 1]) => {:a 1}
             (extra-paramater-map [:a]) =>  (throws IllegalArgumentException)))

(facts "About get-reply-handler"
       (fact "get-reply-handler Returns function"
             (get-reply-handler ..a..) => fn?)
       )
