(ns myyoutube.ui.events
  (:require [re-frame.core :as re-frame]
            [myyoutube.youtube-api :as api]
            [myyoutube.local-storage :as ls]
            [clojure.string :as string]
            [myyoutube.ui.db :as db]))

;;COFX

;;FX

(re-frame/reg-fx
 :init-gapi
 (fn [[client-id update-sign-status]]
   (api/init client-id update-sign-status)))

(re-frame/reg-fx
 :api-get-popular
 (fn [code]
   (api/popular code)))

(re-frame/reg-fx
 :api-get-subscriptions
 (fn []
   (api/subscriptions)))

(re-frame/reg-fx
 :refresh-page
 (fn []
   (.reload js/location)))

;;EVENTS

(re-frame/reg-event-fx
 :initialize-db
 [(ls/persist-db-keys db/storage-key db/store-keys)]
 (fn [_ _]
   {:db db/app-db}))

(re-frame/reg-event-db
 :set
 (fn [db [_ k v]]
   (assoc db k v)))

(re-frame/reg-event-db
 :set-in
 (fn [db [_ path v]]
   (assoc-in db path v)))

(re-frame/reg-event-fx
 :get-popular
 (fn [_ [_ code]]
   {:api-get-popular code}))

(re-frame/reg-event-fx
 :get-subscriptions
 (fn [_ _]
   {:api-get-subscriptions nil}))

(re-frame/reg-event-fx
 :get-api
 (fn [{db :db} _]
   (let [{:keys [items]} (ls/get-storage db)]
     {:dispatch-n (for [{:keys [type country]} items]
                    (case type
                      :popular [:get-popular country]
                      :subscriptions [:get-subscriptions]))})))

(re-frame/reg-event-fx
 :block-channel
 (fn [{db :db} [_ channel-id]]
   {:db (update-in db [db/storage-key :filter] conj channel-id)}))

(re-frame/reg-event-fx
 :save-filters
 (fn [{{:keys [settings-form] :as db} :db} _]
   (let [filters-edit (get-in settings-form [:data :filter])
         filter (string/split filters-edit #" ")]
     {:db (-> (ls/update-storage db :filter filter)
              (assoc :settings-form nil))})))

(defn update-sign-status [signed-in?]
  (re-frame/dispatch [:set :signed-in? signed-in?])
  (re-frame/dispatch [:set :initialized? true])
  (when signed-in?
    (re-frame/dispatch [:get-api])))

(re-frame/reg-event-fx
 :init-client
 (fn [{db :db} _]
   (let [{:keys [client-id]} (ls/get-storage db)]
     (when client-id
       {:init-gapi [client-id update-sign-status]}))))

(re-frame/reg-event-fx
 :refresh-client
 (fn [{db :db} _]
   {:db (ls/update-storage db :client-id nil)
    :refresh-page nil}))

(re-frame/reg-event-fx
 :add-new
 (fn [{{:keys [settings-form] :as db} :db} _]
   (let [{:keys [type country]} (:data settings-form)]
     (merge {:db (-> (update-in db [db/storage-key :items] conj (merge {:type type} (when country {:country country})))
                     (assoc :settings-form nil))}
            (case type
              :popular {:api-get-popular country}
              :subscriptions {:api-get-subscriptions nil})))))

(re-frame/reg-event-fx
 :delete-item
 (fn [{{:keys [settings-form] :as db} :db} _]
   (let [{:keys [type country]} (:data settings-form)
         items (get-in db [db/storage-key :items])]
     {:db (-> (assoc-in db [db/storage-key :items] (remove #(if (= :popular type)
                                                              (and (= type (:type %)) (= country (:country %)))
                                                              (= type (:type %)))
                                                           items))
              (assoc :settings-form nil))})))
