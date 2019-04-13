(ns myyoutube.ui.popup.views
  (:require-macros [myyoutube.utils :refer [defview letsubs]])
  (:require [myyoutube.ui.components :as c]
            [re-frame.core :as re-frame]
            [clojure.string :as string]
            [myyoutube.contries :as contries]))

(defn- get-val [obj] (.-value (.-target obj)))

(defn- get-check [obj] (.-checked (.-target obj)))

(defview quota-view [oppo-color]
  (letsubs [{:keys [type channels]} [:get-in [:settings-form :data]]]
    [:div {:style {:color oppo-color :margin-top 10}} "Quota : " (if (= :popular type) "3" (* (inc (count channels)) 3))]))

(defn add-popular [new? {:keys [country]}]
  (when new?
    [c/view {:margin-top 10}
     [:select {:defaultValue country :on-change #(re-frame/dispatch [:set-in [:settings-form :data :country] (get-val %)])}
      contries/contries-views]]))

(defview add-subsscriptions [new? {:keys [channels]}]
  (letsubs [oppo-color [:oppo-color]
            subscriptions [:get-in [:storage :api :subscriptions]]]
    [c/view {:margin-top 10 :flex 1 :overflow :auto}
     (for [{:keys [title channel-id thumb]} subscriptions]
       ^{:key title}
       [c/view {:flex-direction :row :align-items :center}
        [:input {:type :checkbox :default-checked (boolean (get channels channel-id))
                 :on-click #(re-frame/dispatch [:subscription-item channel-id (get-check %)])}]
        [:img {:src thumb}]
        [:div {:style {:color oppo-color}} title]])]))

(defn item-type-options [new? {:keys [type country]} oppo-color]
  (if new?
    [:div
     [:select {:on-change (fn [obj]
                            (let [type (keyword (get-val obj))]
                              (when (= type :subscriptions)
                                (re-frame/dispatch [:check-subscriptions]))
                              (re-frame/dispatch [:set-in [:settings-form :data :type] type])))}
      [:option {:value :popular} "Popular"]
      [:option {:value :subscriptions} "Subscriptions"]]]
      ;[:option {:value :custom} "Custom"]]]]
    [:div {:style {:color oppo-color }} (if (= type :popular) (str "Popular " country) "Subscriptions")]))

(defn add-edit-view [new? {:keys [type name dont-refresh? compact?] :as data} oppo-color]
  [c/view {:margin 30}
   [c/view {:margin-top 10}
    [c/view {:flex-direction :row :padding-bottom 10}
     [:div {:style {:color oppo-color :padding-right 10}} "Name"]
     [:input {:default-value name :on-change #(re-frame/dispatch [:set-in [:settings-form :data :name] (get-val %)])}]]
    [c/view {:flex-direction :row :padding-bottom 10}
     [:input {:type :checkbox :default-checked dont-refresh?
              :on-click #(re-frame/dispatch [:set-in [:settings-form :data :dont-refresh?] (get-check %)])}]
     [:div {:style {:color oppo-color :padding-right 10}} "Don't request on page load"]]
    [c/view {:flex-direction :row :padding-bottom 10}
     [:input {:type :checkbox :default-checked compact?
              :on-click #(re-frame/dispatch [:set-in [:settings-form :data :compact?] (get-check %)])}]
     [:div {:style {:color oppo-color :padding-right 10}} "Compact UI"]]]
   [item-type-options new? data oppo-color]
   (case type
     :popular [add-popular new? data]
     :subscriptions [add-subsscriptions new? data]
     [:div])
   [quota-view oppo-color]
   (when-not new?
     [c/button {:on-press #(re-frame/dispatch [:delete-item])
                :style    {:margin-top 30 :justify-content :center}} "Delete"])
   (when (and name (not (string/blank? name)))
     [c/button {:on-press #(re-frame/dispatch [:save-item])
                :style    {:margin-top 30 :justify-content :center}} (if new? "Add" "Save")])])

(defview filters-view [data]
  (letsubs [filter [:storage/filter]]
    [c/view {:padding 20}
     [:textarea {:default-value (string/join " " filter)
                 :style         {:width 400 :height 300}
                 :on-change     #(re-frame/dispatch [:set-in [:settings-form :data :filter] (get-val %)])}]
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
          [add-edit-view true data oppo-color]
          :edit-item
          [add-edit-view false data oppo-color])]])))