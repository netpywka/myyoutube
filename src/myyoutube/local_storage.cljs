(ns myyoutube.local-storage
  (:require [alandipert.storage-atom :refer [local-storage]]))

(defonce storage (local-storage (atom {}) :storage))

(defn get-from-storage [key]
  (get @storage key))

(defn save [key value]
  (when (and key value)
    (swap! storage assoc key value)))