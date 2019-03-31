(ns myyoutube.ui.items.views
  (:require-macros [myyoutube.utils :refer [defview letsubs]])
  (:require [myyoutube.ui.components :as c]
            [re-frame.core :as re-frame]))

(defn list-of-videos [items mctrclr block?]
  [c/view {:overflow-y :scroll :padding-right 15}
   (for [{:keys [snippet id]} items]
     ^{:key snippet}
     [:div {:style {:margin 0 :margin-top 10}}
      [c/touchable {:on-press #(.open js/window (str "https://www.youtube.com/watch?v=" id) "_blank")}
       [:img {:src (get-in snippet [:thumbnails :medium :url]) :width 320 :height 180}]]
      [c/view {:flex-direction :row :max-width 320}
       [:div {:style {:font-weight :bold :font-size 13 :color mctrclr}}
        (:channelTitle snippet)
        [:div {:style {:font-weight :normal :font-size 13 :color mctrclr}} (:title snippet)]]
       [c/view {:flex 1}]
       (when block?
         [c/touchable {:on-press #(re-frame/dispatch [:block-channel (:channelId snippet)])} "🚫"])]])])

(defview popular [code]
  (letsubs [items [:popular-filtered code]
            oppo-color [:oppo-color]]
    [c/view {:margin-right 10 :padding-top 10}
     [c/view {:flex-direction :row :align-items :center}
      [c/touchable {:on-press #(re-frame/dispatch [:set :settings-form {:type :edit-item
                                                                        :data {:type :popular :country code}}])} "⚙️"]
      [:div {:style {:color oppo-color :font-size 15}} "Popular "code" videos: " (count items)]]
     [list-of-videos items oppo-color true]]))

(defview subscr []
  (letsubs [items [:sorted-subscriptions]
            oppo-color [:oppo-color]]
    [c/view {:margin-right 10 :padding-top 10}
     [c/view {:flex-direction :row :align-items :center}
      [c/touchable {:on-press #(re-frame/dispatch [:set :settings-form {:type :edit-item
                                                                        :data {:type :subscriptions}}])} "⚙️"]
      [:div {:style {:color oppo-color :font-size 15}} "Subscriptions: " (count items)]]
     [list-of-videos items oppo-color false]]))

(defview items-view []
  (letsubs [items [:storage/items]]
    [c/view {:flex-direction :row :flex 1 :overflow :hidden :overflow-x :scroll}
     (for [{:keys [type country]} items]
       (case type
         :popular ^{:key (str type country)} [popular country]
         :subscriptions ^{:key type} [subscr]))]))
