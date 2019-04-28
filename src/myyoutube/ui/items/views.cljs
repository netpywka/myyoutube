(ns myyoutube.ui.items.views
  (:require-macros [myyoutube.utils :refer [defview letsubs]])
  (:require [myyoutube.ui.components :as c]
            [re-frame.core :as re-frame]))

(defn get-w-h [compact?]
  (let [w (if compact? (/ 320 1.5) 320)
        h (if compact? (/ 180 1.5) 180)]
    {:w w :h h}))

(defn video-item [{:keys [id thumb channel-id channel-title title seen?]}
                  oppo-color block? {:keys [compact?]}]
  (let [{:keys [w h]} (get-w-h compact?)]
    [:div {:style {:margin 0 :margin-top 10 :opacity (if seen? 0.5 1)}}
     [c/touchable {:on-press #(re-frame/dispatch [:open-video id])}
      [:img {:src thumb :width w :height h}]]
     [c/view {:flex-direction :row :max-width w}
      [:div {:style {:font-weight :bold :font-size 13 :color oppo-color}}
       channel-title
       [:div {:style {:font-weight :normal :font-size 13 :color oppo-color}} title]]
      [c/view {:flex 1}]
      (when block?
        [c/touchable {:on-press #(re-frame/dispatch [:block-channel channel-id])}
         [:img {:src "./assets/block.svg" :width 12 :height 12}]])]]))

(defn edit-button [data]
  [c/touchable {:on-press #(re-frame/dispatch [:edit-item data])}
   [:img {:src "./assets/menu.svg" :width 15 :height 15}]])

(defn refresh-button [{:keys [refresh?] :as data}]
  [:div {:style {:opacity (if refresh? 0.5 1)}}
   [c/touchable {:on-press #(re-frame/dispatch [:get-api-for-item data])}
    [:img {:src "./assets/refresh.svg" :width 15 :height 15}]]])

(defn list-of-videos [items oppo-color block? {:keys [name compact?] :as data} loading?]
  (let [{:keys [w]} (get-w-h compact?)]
    [c/view {:margin-right 10 :padding-top 10}
     (when loading?
       [c/view {:align-items :center}
        [:div {:class :loader}]])
     [c/view {:flex-direction :row :align-items :center :max-width w :justify-content :space-between}
      [refresh-button data]
      [:div {:style {:color oppo-color :font-size 15 :font-weight :bold}} name " " (count items)]
      [edit-button data]]
     [c/view {:overflow-y :scroll :padding-right 15}
      (for [{:keys [id] :as item} items]
        ^{:key (str id item)}
        [video-item item oppo-color block? data])]]))

(defview popular [{:keys [country] :as data}]
  (letsubs [items      [:popular-filtered-seen country]
            loading?   [:popular-loading country]
            oppo-color [:oppo-color]]
    [list-of-videos items oppo-color true data loading?]))

(defview subscr [{:keys [id] :as data}]
  (letsubs [items      [:sorted-playlists-seen id]
            loading?   [:channels-loading id]
            oppo-color [:oppo-color]]
    [list-of-videos items oppo-color false data loading?]))

(defview items-view []
  (letsubs [items [:storage/items]]
    [c/view {:flex-direction :row :flex 1 :overflow :hidden :overflow-x :scroll}
     (for [{:keys [type id] :as data} (vals items)]
       (case type
         :popular ^{:key id} [popular data]
         :subscriptions ^{:key id} [subscr data]))]))