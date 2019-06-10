(ns book.ui-routing
  (:require [com.fulcrologic.fulcro.routing.union-router :as r :refer-macros [defrouter]]
            [com.fulcrologic.fulcro.dom :as dom]
            [fulcro.client :as fc]
            [com.fulcrologic.fulcro.data-fetch :as df]
            [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
            [com.fulcrologic.fulcro.mutations :as m]))

(defsc Main [this {:keys [label]}]
  {:initial-state {:page :main :label "MAIN"}
   :query         [:page :label]}
  (dom/div {:style {:backgroundColor "red"}}
    label))

(defsc Login [this {:keys [label]}]
  {:initial-state {:page :login :label "LOGIN"}
   :query         [:page :label]}
  (dom/div {:style {:backgroundColor "green"}}
    label))

(defsc NewUser [this {:keys [label]}]
  {:initial-state {:page :new-user :label "New User"}
   :query         [:page :label]}
  (dom/div {:style {:backgroundColor "skyblue"}}
    label))

(defsc StatusReport [this {:keys [id]}]
  {:initial-state {:id :a :page :status-report}
   :query         [:id :page :label]}
  (dom/div {:style {:backgroundColor "yellow"}}
    (dom/div (str "Status " id))))

(defsc GraphingReport [this {:keys [id]}]
  {:initial-state {:id :a :page :graphing-report}
   :query         [:id :page :label]}                       ; make sure you query for everything need by the router's ident function!
  (dom/div {:style {:backgroundColor "orange"}}
    (dom/div (str "Graph " id))))

(defrouter ReportRouter :report-router
  ; This router expects numerous possible status and graph reports. The :id in the props of the report will determine
  ; which specific data set is used for the screen (though the UI of the screen will be either StatusReport or GraphingReport
  ; IMPORTANT: Make sure your components (e.g. StatusReport) query for what ident needs (i.e. in this example
  ; :page and :id at a minimum)
  [:page :id]
  :status-report StatusReport
  :graphing-report GraphingReport)

(def ui-report-router (comp/factory ReportRouter))

; BIG GOTCHA: Make sure you query for the prop (in this case :page) that the union needs in order to decide. It won't pull it itself!
(defsc ReportsMain [this {:keys [report-router]}]
  ; nest the router under any arbitrary key, just be consistent in your query and props extraction.
  {:initial-state (fn [params] {:page :report :report-router (comp/get-initial-state ReportRouter {})})
   :query         [:page {:report-router (comp/get-query ReportRouter)}]}
  (dom/div {:style {:backgroundColor "grey"}}
    ; Screen-specific content to be shown "around" or "above" the subscreen
    "REPORT MAIN SCREEN"
    ; Render the sub-router. You can also def a factory for the router (e.g. ui-report-router)
    (ui-report-router report-router)))

(defrouter TopRouter :top-router
  (fn [this props] [(:page props) :top])
  :main Main
  :login Login
  :new-user NewUser
  :report ReportsMain)

(def ui-top (comp/factory TopRouter))

(def routing-tree
  "A map of route handling instructions. The top key is the handler name of the route which can be
  thought of as the terminal leaf in the UI graph of the screen that should be \"foremost\".

  The value is a vector of routing-instructions to tell the UI routers which ident
  of the route that should be made visible.

  A value in this ident using the `param` namespace will be replaced with the incoming route parameter
  (without the namespace). E.g. the incoming route-param :report-id will replace :param/report-id"
  (r/routing-tree
    (r/make-route :main [(r/router-instruction :top-router [:main :top])])
    (r/make-route :login [(r/router-instruction :top-router [:login :top])])
    (r/make-route :new-user [(r/router-instruction :top-router [:new-user :top])])
    (r/make-route :graph [(r/router-instruction :top-router [:report :top])
                          (r/router-instruction :report-router [:graphing-report :param/report-id])])
    (r/make-route :status [(r/router-instruction :top-router [:report :top])
                           (r/router-instruction :report-router [:status-report :param/report-id])])))

(defsc Root [this {:keys [top-router]}]
  ; r/routing-tree-key implies the alias of com.fulcrologic.fulcro.routing.union-router as r.
  {:initial-state (fn [params] (merge routing-tree
                                 {:top-router (comp/get-initial-state TopRouter {})}))
   :query         [{:top-router (comp/get-query TopRouter)}]}
  (dom/div
    ; Sample nav mutations
    (dom/a {:onClick #(comp/transact! this `[(r/route-to {:handler :main})])} "Main") " | "
    (dom/a {:onClick #(comp/transact! this `[(r/route-to {:handler :new-user})])} "New User") " | "
    (dom/a {:onClick #(comp/transact! this `[(r/route-to {:handler :login})])} "Login") " | "
    (dom/a {:onClick #(comp/transact! this `[(r/route-to {:handler :status :route-params {:report-id :a}})])} "Status A") " | "
    (dom/a {:onClick #(comp/transact! this `[(r/route-to {:handler :graph :route-params {:report-id :a}})])} "Graph A")
    (ui-top top-router)))


