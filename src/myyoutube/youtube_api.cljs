(ns myyoutube.youtube-api
  (:require [re-frame.core :as re-frame]
            [cljs-time.core :as time]
            [cljs-time.format :as format]
            [clojure.string :as string]))

(def API-KEY "")

(def token-client (atom nil))
(def access-token (atom nil))

(defn init [client-id update-sign-status]
  (reset! token-client (.initTokenClient js/google.accounts.oauth2
                                         (clj->js {:client_id     client-id
                                                   :scope         "https://www.googleapis.com/auth/youtube.readonly"
                                                   :callback      (fn [resp]
                                                                    (reset! access-token (.-access_token resp))
                                                                    (update-sign-status true))})))
  (if-let [token (.getToken js/gapi.client)]
    (do
      (reset! access-token token)
      (update-sign-status true))
    (.requestAccessToken @token-client)))

(defn sing-in [])
  ;(.signIn (auth-instance)))

(defn sing-out [])
  ;(.signOut (auth-instance)))

(defn request-all-list [{:keys [api params items callback once? quota] :or {items []} :as obj}]
  (re-frame/dispatch [:update-quota quota])
  (-> (.list api (clj->js params))
      (.then #(let [{:keys [result]} (js->clj % :keywordize-keys true)
                    {:keys [nextPageToken]} result
                    items (into [] (concat items (:items result)))]
                (if (and nextPageToken (not once?))
                  (request-all-list (-> obj
                                        (assoc :items items)
                                        (assoc-in [:params "pageToken"] nextPageToken)))
                  (callback items))))))

(defn subscriptions []
  (request-all-list
   {:api      js/gapi.client.youtube.subscriptions
    :quota    3
    :params   {"mine"       "true"
               "part"       "snippet"
               "maxResults" "50"
               "key"        API-KEY}
    :callback #(do
                 (re-frame/dispatch [:set-in [:loading :subscriptions] false])
                 (re-frame/dispatch [:store-subscriptions %]))}))

(defn popular [code]
  (request-all-list
   {:api      js/gapi.client.youtube.videos
    :quota    3
    :params   {"chart"      "mostPopular"
               "regionCode" code
               "part"       "snippet"
               "maxResults" "50"
               "key"        API-KEY}
    :callback #(do
                 (re-frame/dispatch [:set-in [:loading :popular code] false])
                 (re-frame/dispatch [:store-api [:popular code] %]))}))

(defn sanitize-time [s]
  (-> (string/trim s)
      (string/replace #"[\s\n]+" "")))

(defn channels [id items]
  (re-frame/dispatch [:update-quota (* (count items) 3)])
  (-> (let [batch (js/gapi.client.newBatch.)]
        (doseq [channel-id items]
          (.add batch (.list js/gapi.client.youtube.playlistItems #js {"playlistId" (str "UU" (subs channel-id 2))
                                                                       "maxResults" 5
                                                                       "part"       "contentDetails"
                                                                       "key"        API-KEY})))
        batch)
      (.then (fn [result]
               (let [play-lists                   (vals (js->clj (aget result "result") :keywordize-keys true))
                     videos                       (flatten (map #(get-in % [:result :items]) play-lists))
                     week                         (time/minus (time/now) (time/weeks 1))
                     videos                       (map #(update-in % [:contentDetails :videoPublishedAt] sanitize-time) videos)
                     last-week-videos             (filter (fn [{:keys [contentDetails]}]
                                                            (let [pushed (format/parse (format/formatters :date-time-no-ms)
                                                                                       (:videoPublishedAt contentDetails))]
                                                              (time/after? pushed week)))
                                                          videos)
                     sorted-by-upload-time-videos (sort-by #(format/parse (format/formatters :date-time-no-ms)
                                                                          (get-in % [:contentDetails :videoPublishedAt]))
                                                           time/after?
                                                           last-week-videos)
                     video-ids                    (map #(get-in % [:contentDetails :videoId]) sorted-by-upload-time-videos)]
                 (request-all-list
                  {:api      js/gapi.client.youtube.videos
                   :quota    3
                   :params   {"id"   (string/join "," (take 50 video-ids))
                              "part" "snippet"}
                   :callback (fn [videos]
                               (do
                                 (re-frame/dispatch [:set-in [:loading :channels id] false])
                                 (re-frame/dispatch [:store-api [:playlists id] videos])))
                   :once?    true}))))))
