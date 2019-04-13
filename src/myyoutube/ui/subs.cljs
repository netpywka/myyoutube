(ns myyoutube.ui.subs
  (:require [re-frame.core :as re-frame]
            [cljs-time.format :as format]
            [cljs-time.core :as time]))

(re-frame/reg-sub
 :get
 (fn [db [_ k]]
   (get db k)))

(re-frame/reg-sub
 :get-in
 (fn [db [_ path]]
   (get-in db path)))

(re-frame/reg-sub
 :get-api-by-code
 :<- [:storage/api]
 (fn [api [_ path]]
   (get-in api path)))

(re-frame/reg-sub
 :popular-filtered
 (fn [[_ code] _]
   [(re-frame/subscribe [:get-api-by-code [:popular code]])
    (re-frame/subscribe [:storage/filter])])
 (fn [[popular filter ] _]
   (if (empty? filter)
     popular
     (remove #(filter (:channel-id %)) popular))))

(re-frame/reg-sub
 :popular-filtered-seen
 (fn [[_ code] _]
   [(re-frame/subscribe [:popular-filtered code])
    (re-frame/subscribe [:storage/seen])])
 (fn [[popular seen] _]
   (map #(assoc % :seen? (seen (:id %))) popular)))

(re-frame/reg-sub
 :subscriptions
 :<- [:storage/api]
 (fn [api _]
   (get api :subscriptions)))

(re-frame/reg-sub
 :playlists
 :<- [:storage/api]
 (fn [api _]
   (get api :playlists)))

(re-frame/reg-sub
 :playlists-videos
 :<- [:playlists]
 (fn [playlists [_ id]]
   (get playlists id)))

(re-frame/reg-sub
 :sorted-playlists
 (fn [[_ id] _]
   [(re-frame/subscribe [:playlists-videos id])])
 (fn [[playlists] _]
   (let [format (format/formatters :date-time)]
     (sort-by #(format/parse format (:published-at %))
              time/after?
              playlists))))

(re-frame/reg-sub
 :sorted-playlists-seen
 (fn [[_ id] _]
   [(re-frame/subscribe [:sorted-playlists id])
    (re-frame/subscribe [:storage/seen])])
 (fn [[playlists seen] _]
   (map #(assoc % :seen? (seen (:id %))) playlists)))

(re-frame/reg-sub
 :color
 :<- [:storage/bg]
 (fn [bg _]
   (if bg :black :white)))

(re-frame/reg-sub
 :oppo-color
 :<- [:storage/bg]
 (fn [bg _]
   (if bg :white :black)))