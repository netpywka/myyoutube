(ns myyoutube.events
  (:require [re-frame.core :as re-frame]
            [myyoutube.youtube-api :as api]
            [myyoutube.local-storage :as ls]))
(re-frame/reg-event-db
 :initialize-db
 (fn [_ _]
   {:signed-in? false}))

(re-frame/reg-event-db
 :set
 (fn [db [_ k v]]
   (assoc db k v)))

(re-frame/reg-event-db
 :set-in
 (fn [db [_ path v]]
   (assoc-in db path v)))

(re-frame/reg-fx
 :api-get-popilar
 (fn []
    (api/popular "RU")
    (api/popular "US")))

(re-frame/reg-fx
 :api-get-subscriptions
 (fn []
   (api/subscriptions)))

(re-frame/reg-event-fx
 :get-api
 (fn [_ _]
   {:api-get-popilar       nil
    :api-get-subscriptions nil}))

(re-frame/reg-fx
 :store-filters
 (fn [filters]
   (ls/save :filters filters)))

(re-frame/reg-event-fx
 :block-channel
 (fn [{db :db} [_ channel-id]]
   (let [db' (update db :filter conj channel-id)]
     {:db db'
      :store-filters (:filter db')})))

(re-frame/reg-cofx
 :get-filters-from-storage
 (fn [cofx _]
   (assoc cofx :filters (ls/get-from-storage :filters))))

(re-frame/reg-event-fx
 :initialize
 [(re-frame/inject-cofx :get-filters-from-storage)]
 (fn [{db :db filters :filters} _]
   {:db (assoc db :filter filters)}))