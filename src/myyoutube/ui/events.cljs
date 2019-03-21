(ns myyoutube.ui.events
  (:require [re-frame.core :as re-frame]
            [myyoutube.youtube-api :as api]
            [myyoutube.local-storage :as ls]))

;;COFX

(re-frame/reg-cofx
 :get-filters-from-storage
 (fn [cofx _]
   (assoc cofx :filters (ls/get-from-storage :filters))))

(re-frame/reg-cofx
 :get-client-id-from-storage
 (fn [cofx _]
   (assoc cofx :client-id (ls/get-from-storage :client-id))))

;;FX

(re-frame/reg-fx
 :init-gapi
 (fn [[client-id update-sign-status]]
   (api/init client-id update-sign-status)))

(re-frame/reg-fx
 :api-get-popular
 (fn []
   (api/popular "RU")
   (api/popular "US")))

(re-frame/reg-fx
 :api-get-subscriptions
 (fn []
   (api/subscriptions)))

(re-frame/reg-fx
 :store
 (fn [[key value]]
   (ls/save key value)))

;;EVENTS

(re-frame/reg-event-db
 :initialize-db
 (fn [_ _]
   {:signed-in? false}))

(re-frame/reg-event-fx
 :initialize
 [(re-frame/inject-cofx :get-filters-from-storage)
  (re-frame/inject-cofx :get-client-id-from-storage)]
 (fn [{db :db filters :filters client-id :client-id} _]
   {:db (assoc db :filter filters :client-id client-id)}))

(re-frame/reg-event-db
 :set
 (fn [db [_ k v]]
   (assoc db k v)))

(re-frame/reg-event-db
 :set-in
 (fn [db [_ path v]]
   (assoc-in db path v)))

(re-frame/reg-event-fx
 :get-api
 (fn [_ _]
   {:api-get-popular       nil}))
    ;:api-get-subscriptions nil}))

(re-frame/reg-event-fx
 :block-channel
 (fn [{db :db} [_ channel-id]]
   (let [db' (update db :filter conj channel-id)]
     {:db db'
      :store [:filters (:filter db')]})))

(defn update-sign-status [signed-in?]
  (re-frame/dispatch [:set :signed-in? signed-in?])
  (re-frame/dispatch [:set :initialized true])
  (when signed-in?
    (re-frame/dispatch [:get-api])))

(re-frame/reg-event-fx
 :init-client
 (fn [{{:keys [client-id]} :db} _]
   (when client-id
     {:store [:client-id client-id]
      :init-gapi [client-id update-sign-status]})))