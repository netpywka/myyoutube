(ns myyoutube.ui.events
  (:require [re-frame.core :as re-frame]
            [myyoutube.youtube-api :as api]
            [myyoutube.local-storage :as ls]
            [clojure.string :as string]
            [myyoutube.ui.db :as db]
            [cljs-time.core :as time]
            [cljs-time.format :as format]))

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
 :api-get-channels
 (fn [[id channels]]
   (api/channels id channels)))

(re-frame/reg-fx
 :refresh-page
 (fn []
   (.reload js/location)))

(re-frame/reg-fx
 :open-video-fx
 (fn [id]
   (.open js/window (str "https://www.youtube.com/watch?v=" id) "_blank")))

;;EVENTS

(re-frame/reg-event-fx
 :initialize-db
 [(ls/persist-db-keys db/storage-key db/store-keys db/default-values)]
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
 :get-channels
 (fn [_ [_ id channels]]
   {:api-get-channels [id channels]}))

(re-frame/reg-event-fx
 :get-api
 (fn [{db :db} _]
   (let [{:keys [items]} (ls/get-storage db)]
     {:dispatch-n (for [{:keys [type country refresh? id channels]} (vals items)]
                    (when refresh?
                      (case type
                        :popular [:get-popular country]
                        :subscriptions [:get-channels id channels])))})))

(re-frame/reg-event-fx
 :get-api-for-item
 (fn [_ [_ {:keys [type country id channels]}]]
   {:dispatch (case type
                :popular [:get-popular country]
                :subscriptions [:get-channels id channels])}))

(re-frame/reg-event-fx
 :block-channel
 (fn [{db :db} [_ channel-id]]
   {:db (update-in db [db/storage-key :filter] conj channel-id)}))

(re-frame/reg-event-fx
 :save-filters
 (fn [{{:keys [settings-form] :as db} :db} _]
   (let [filters-edit (get-in settings-form [:data :filter])
         filter       (set (string/split filters-edit #" "))]
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
   {:db           (ls/update-storage db :client-id nil)
    :refresh-page nil}))

(re-frame/reg-event-fx
 :save-item
 (fn [{{:keys [settings-form] :as db} :db} _]
   (let [{:keys [id type country name dont-refresh? compact? channels]} (:data settings-form)
         {:keys [items]} (ls/get-storage db)
         id (or id (if (seq items) (inc (apply max (keys items))) 0))]
     (merge {:db (-> (assoc-in db [db/storage-key :items id]
                               (merge {:id       id :type type :name name
                                       :refresh? (not dont-refresh?) :compact? compact?}
                                      (when channels {:channels (keys channels)})
                                      (when country {:country country})))
                     (assoc :settings-form nil))}
            (case type
              :popular {:api-get-popular country}
              :subscriptions {:api-get-channels [id (keys channels)]})))))

(defn normalize-data [{:keys [refresh? channels] :as data}]
  (assoc data :dont-refresh? (not refresh?) :channels (into {} (map #(hash-map % "") channels))))

(re-frame/reg-event-fx
 :edit-item
 (fn [{db :db} [_ data]]
   {:db (assoc db :settings-form {:type  :edit-item
                                  :title "Edit"
                                  :data  (normalize-data data)})}))

(re-frame/reg-event-fx
 :delete-item
 (fn [{{:keys [settings-form] :as db} :db} _]
   (let [{:keys [id]} (:data settings-form)]
     {:db (-> (update-in db [db/storage-key :items] dissoc id)
              (assoc :settings-form nil))})))

(defn curr-time-pt []
  (time/minus (time/now) (time/hours 7)))

(re-frame/reg-event-fx
 :init-quota
 (fn [{db :db} _]
   (when-let [last-time (get-in db [db/storage-key :quota :time])]
     (let [last-time (format/parse (format/formatters :date-time) last-time)
           cur-time  (curr-time-pt)
           mid-night (time/today-at-midnight)]
       (when (and (time/before? last-time mid-night)
                  (time/after? cur-time mid-night))
         {:db (assoc-in db [db/storage-key :quota] nil)})))))

(re-frame/reg-event-fx
 :update-quota
 (fn [{db :db} [_ quota]]
   (let [cur-quota (get-in db [db/storage-key :quota :number])]
     {:db (assoc-in db [db/storage-key :quota] {:time   (format/unparse (format/formatters :date-time) (curr-time-pt))
                                                :number (+ quota cur-quota)})})))

(re-frame/reg-event-fx
 :check-subscriptions
 (fn [{db :db} _]
   (when-not (get-in db [:api :subscriptions])
     {:api-get-subscriptions nil})))

(re-frame/reg-event-fx
 :subscription-item
 (fn [{db :db} [_ ch-id selected?]]
   {:db (update-in db [:settings-form :data :channels] (if selected? assoc dissoc) ch-id "")}))

(re-frame/reg-event-fx
 :open-video
 (fn [{db :db} [_ id]]
   {:db            (update-in db [db/storage-key :seen] conj id)
    :open-video-fx id}))

(re-frame/reg-event-fx
  :store-subscriptions
  (fn [{db :db} [_ items]]
    {:db (assoc-in db
                   [db/storage-key :api :subscriptions]
                   (map (fn [value]
                          {:title (get-in value [:snippet :title])
                           :channel-id (get-in value [:snippet :resourceId :channelId])
                           :thumb (get-in value [:snippet :thumbnails :default :url])})
                        items))}))

(re-frame/reg-event-fx
 :store-api
 (fn [{db :db} [_ path items]]
   {:db (assoc-in db
                  (into [] (concat [db/storage-key :api] path))
                  (map (fn [value]
                         {:id (get value :id)
                          :published-at (get-in value [:snippet :publishedAt])
                          :thumb (get-in value [:snippet :thumbnails :medium :url])
                          :channel-title (get-in value [:snippet :channelTitle])
                          :channel-id (get-in value [:snippet :channelId])
                          :title  (get-in value [:snippet :title])})
                       items))}))