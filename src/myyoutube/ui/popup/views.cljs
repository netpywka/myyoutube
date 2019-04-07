(ns myyoutube.ui.popup.views
  (:require-macros [myyoutube.utils :refer [defview letsubs]])
  (:require [myyoutube.ui.components :as c]
            [re-frame.core :as re-frame]
            [clojure.string :as string]
            [myyoutube.contries :as contries]))

(defview quota-view [oppo-color]
  (letsubs [{:keys [type channels]} [:get-in [:settings-form :data]]]
    [:div {:style {:color oppo-color :margin-top 10}} "Quota : " (if (= :popular type) "3" (* (inc (count channels)) 3))]))

(defn add-popular []
  [c/view {:margin-top 10}
   [:select {:on-change #(re-frame/dispatch [:set-in [:settings-form :data :country] (.-value (.-target %))])}
    contries/contries-views]])

(defview add-subsscriptions []
  (letsubs [oppo-color [:oppo-color]
            subscriptions [:get-in [:api :subscriptions]]]
    [c/view {:margin-top 10 :flex 1 :overflow :auto}
     (for [{:keys [snippet]} subscriptions]
       (let [{:keys [title]} snippet
             url (get-in snippet [:thumbnails :default :url])
             channel-id (get-in snippet [:resourceId :channelId])]
         ^{:key title}
         [c/view {:flex-direction :row :align-items :center}
          [:input {:type :checkbox :on-click #(re-frame/dispatch [:subscription-item channel-id (.-checked (.-target %))])}]
          [:img {:src url}]
          [:div {:style {:color oppo-color}} title]]))]))

(defview add-new-view [{:keys [type]}]
  (letsubs [oppo-color [:oppo-color]
            {:keys [data]} [:get :settings-form]]
    (let [{:keys [name]} data]
      [c/view {:margin 30}
       [c/view {:margin-top 10}
        [c/view {:flex-direction :row :padding-bottom 10}
         [:div {:style {:color oppo-color :padding-right 10}} "Name"]
         [:input {:on-change #(re-frame/dispatch [:set-in [:settings-form :data :name] (.-value (.-target %))])}]]
        [c/view {:flex-direction :row :padding-bottom 10}
         [:input {:type :checkbox :on-click #(re-frame/dispatch [:set-in [:settings-form :data :dont-refresh?] (.-checked (.-target %))])}]
         [:div {:style {:color oppo-color :padding-right 10}} "Don't request on page load"]]
        [c/view {:flex-direction :row :padding-bottom 10}
         [:input {:type :checkbox :on-click #(re-frame/dispatch [:set-in [:settings-form :data :compact?] (.-checked (.-target %))])}]
         [:div {:style {:color oppo-color :padding-right 10}} "Compact UI"]]
        [:select {:on-change (fn [obj]
                               (let [type (keyword (.-value (.-target obj)))]
                                 (when (= type :subscriptions)
                                   (re-frame/dispatch [:check-subscriptions]))
                                 (re-frame/dispatch [:set-in [:settings-form :data :type] type])))}
         [:option {:value :popular} "Popular"]
         [:option {:value :subscriptions} "Subscriptions"]]]
         ;[:option {:value :custom} "Custom"]]]
       (case type
         :popular [add-popular]
         :subscriptions [add-subsscriptions]
         [:div])
       [quota-view oppo-color]
       (when (and name (not (string/blank? name)))
         [c/button {:on-press #(re-frame/dispatch [:add-new])
                    :style    {:margin-top 30 :justify-content :center}} "Add"])])))

(defview edit-item [{:keys [type country]}]
  (letsubs [oppo-color [:oppo-color]]
    [c/view {:margin 30}
     [c/view {:color oppo-color} (if (= :popular type) (str "Popular " country) "Subscriptions")]
     [c/view {:margin-top 10}]
     [c/button {:on-press #(re-frame/dispatch [:delete-item])
                :style    {:margin-top 30 :justify-content :center}} "Delete"]]))

(defview filters-view [data]
  (letsubs [filter [:storage/filter]]
    [c/view {:padding 20}
     [:textarea {:default-value (string/join " " filter)
                 :style         {:width 400 :height 300}
                 :on-change     #(re-frame/dispatch [:set-in [:settings-form :data :filter] (.-value (.-target %))])}]
     (when data
       [c/button {:on-press #(re-frame/dispatch [:save-filters]) :style {:width 150 :margin-top 10}} "Save"])]))

(defview popup []
  (letsubs [{:keys [type data title] :as form} [:get :settings-form]
            color  [:color]
            oppo-color [:oppo-color]]
    (when form
      [c/view {:position    :absolute :left 0 :right 0 :top 0 :bottom 0 :background-color "rgba(0, 0, 0, 0.4)"
               :align-items :center :justify-content :center}
       [c/view {:background-color color :border-radius 4 :margin 50}
        [c/view {:flex-direction :row :align-items :center :padding-left 10}
         [:div {:style {:font-weight :bold :color oppo-color}} title]
         [c/view {:flex 1}]
         [c/button {:on-press #(re-frame/dispatch [:set :settings-form nil])} "x"]]
        (case type
          :filters
          [filters-view data]
          :add-new
          [add-new-view data]
          :edit-item
          [edit-item data])]])))