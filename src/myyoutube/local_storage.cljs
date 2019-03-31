(ns myyoutube.local-storage
  (:require [alandipert.storage-atom :refer [local-storage]]
            [re-frame.core :as re-frame]))

(def storage-key-atom (atom nil))
(def storage-atoms (atom {}))

(defn register-stores [storage-key store-keys]
  (reset! storage-key-atom storage-key)
  (doseq [store-key store-keys]
    (swap! storage-atoms assoc store-key
           (local-storage (atom nil) store-key))))

(defn <-store [store-key]
  @(get @storage-atoms store-key))

(defn ->store [store-key data]
  (reset! (@storage-atoms store-key) data))

(defn persist-db-keys [storage-key store-keys]
  (register-stores storage-key store-keys)
  (re-frame/->interceptor
   :id "persist-db-keys"
   :after (fn [context]
            (doseq [store-key store-keys]
              (let [k (keyword (str (name storage-key) "/" (name store-key)))]
                (re-frame/reg-sub k (fn [db _] (get-in db [storage-key store-key])))
                (add-watch (re-frame/subscribe [k]) k #(->store store-key %4))))
            (re-frame/reg-event-fx
             :ls/store
             (fn [{db :db} [_ key data]]
               {:db (assoc-in db [storage-key key] data)}))
            (update-in context [:effects :db] merge {storage-key
                                                     (into {} (for [store-key store-keys]
                                                                {store-key (<-store store-key)}))}))))

(defn get-storage [db]
  (get db @storage-key-atom))

(defn update-storage [db key data]
  (assoc-in db [@storage-key-atom key] data))