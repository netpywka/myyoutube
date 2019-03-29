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

(defn all-list [api params items callback]
  (-> (.list api (clj->js params))
      (.then #(let [{:keys [result]} (js->clj % :keywordize-keys true)
                    {:keys [nextPageToken]} result
                    items (into [] (concat items (:items result)))]
                (if nextPageToken
                  (all-list api (assoc params "pageToken" nextPageToken) items callback)
                  (callback items))))))

(defn load-videos [items]
  (all-list js/gapi.client.youtube.videos
            {"id"   (string/join "," (take 10 items))
             "part" "snippet"}
            []
            (fn [videos]
              (re-frame/dispatch [:set-in [:api :subscriptions] videos]))))

(defn prepare-list [channel-id contentDetails]
  (.list js/gapi.client.youtube.playlistItems #js {"playlistId" (str "UU" (subs channel-id 2))
                                                   "maxResults" (str (:newItemCount contentDetails))
                                                   "part"       "contentDetails"}))

(defn load-channels [items]
  (-> (let [batch (js/gapi.client.newBatch.)]
        (doseq [{:keys [snippet contentDetails]} items]
          (let [channel-id (get-in snippet [:resourceId :channelId])]
            (.add batch (prepare-list channel-id contentDetails))))
        batch)
      (.then (fn [result]
               (let [week     (time/minus (time/now) (time/weeks 1))
                     ;;TODO weird bug, it doesn't want to convert js object to cljs directly
                     channels (vals (js->clj (js/JSON.parse (js/JSON.stringify (aget result "result")))
                                             :keywordize-keys true))
                     items    (flatten (map #(get-in % [:result :items]) channels))]
                 (load-videos (map #(get-in % [:contentDetails :videoId])
                                   (sort-by #(format/parse (format/formatters :date-time)
                                                           (get-in % [:contentDetails :videoPublishedAt]))
                                            time/after?
                                            (filter (fn [{:keys [contentDetails]}]
                                                      (let [pushed (format/parse (format/formatters :date-time) (:videoPublishedAt contentDetails))]
                                                        (time/after? pushed week)))
                                                    items)))))))))

(defn subscriptions []
  (all-list js/gapi.client.youtube.subscriptions
            {"mine"       "true"
             "part"       "snippet,contentDetails"
             "maxResults" "50"}
            []
            (fn [items]
              ;; load only channels which has new items
              (load-channels (filter #(not (zero? (get-in % [:contentDetails :newItemCount]))) items)))))

(defn popular [code]
  (all-list js/gapi.client.youtube.videos
            {"chart"      "mostPopular"
             "regionCode" code
             "part"       "snippet"
             "maxResults" "50"}
            []
            #(re-frame/dispatch [:set-in [:api :popular code] %])))