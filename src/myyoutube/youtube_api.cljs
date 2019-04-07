(ns myyoutube.youtube-api
  (:require [re-frame.core :as re-frame]
            [cljs-time.core :as time]
            [cljs-time.format :as format]
            [clojure.string :as string]))

(defn auth-instance []
  (.getAuthInstance js/gapi.auth2))

(defn init [client-id update-sign-status]
  (-> (.init js/gapi.client
             (clj->js {:discoveryDocs ["https://www.googleapis.com/discovery/v1/apis/youtube/v3/rest"]
                       :clientId      client-id
                       :scope         "https://www.googleapis.com/auth/youtube.readonly"}))
      (.then (fn []
               (let [signed-in? (.-isSignedIn (auth-instance))]
                 (.listen signed-in? update-sign-status)
                 (update-sign-status (.get signed-in?)))))))

(defn sing-in []
  (.signIn (auth-instance)))

(defn sing-out []
  (.signOut (auth-instance)))

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
               "maxResults" "50"}
    :callback #(re-frame/dispatch [:set-in [:api :subscriptions] %])}))

(defn popular [code]
  (request-all-list
   {:api      js/gapi.client.youtube.videos
    :quota    3
    :params   {"chart"      "mostPopular"
               "regionCode" code
               "part"       "snippet"
               "maxResults" "50"}
    :callback #(re-frame/dispatch [:set-in [:api :popular code] %])}))

(defn channels [id items]
  (re-frame/dispatch [:update-quota (* (count items) 3)])
  (-> (let [batch (js/gapi.client.newBatch.)]
        (doseq [channel-id items]
          (.add batch (.list js/gapi.client.youtube.playlistItems #js {"playlistId" (str "UU" (subs channel-id 2))
                                                                       "maxResults" 5
                                                                       "part"       "contentDetails"})))
        batch)
      (.then (fn [result]
               (let [;;TODO weird bug, it doesn't want to convert js object to cljs directly
                     play-lists                   (vals (js->clj (js/JSON.parse (js/JSON.stringify (aget result "result")))
                                                                 :keywordize-keys true))
                     videos                       (flatten (map #(get-in % [:result :items]) play-lists))
                     week                         (time/minus (time/now) (time/weeks 1))
                     last-week-videos             (filter (fn [{:keys [contentDetails]}]
                                                            (let [pushed (format/parse (format/formatters :date-time) (:videoPublishedAt contentDetails))]
                                                              (time/after? pushed week)))
                                                          videos)
                     sorted-by-upload-time-videos (sort-by #(format/parse (format/formatters :date-time)
                                                                          (get-in % [:contentDetails :videoPublishedAt]))
                                                           time/after?
                                                           last-week-videos)
                     video-ids                    (map #(get-in % [:contentDetails :videoId]) sorted-by-upload-time-videos)]
                 (println video-ids)
                 (request-all-list
                  {:api      js/gapi.client.youtube.videos
                   :quota    3
                   :params   {"id"   (string/join "," (take 50 video-ids))
                              "part" "snippet"}
                   :callback (fn [videos]
                               (re-frame/dispatch [:set-in [:api :playlists id] videos]))
                   :once?    true}))))))