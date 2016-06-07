(ns autotc-web.views.layout
  (:require [hiccup.page :refer [html5 include-css include-js]]
            [clojure.string :as cljstr]))

(defn common [& body]
  (html5
   [:head
    [:title "autotc"]
    (include-css "/css/bootstrap.min.css")
    (include-css "/css/gh-fork-ribbon.css")
    (include-css "/css/gh-fork-ribbon.ie.css")
    (include-css "/css/style.css")
    (include-js "/js/lib/jquery-2.1.3.min.js")
    (include-js "/js/lib/react-0.14.5.min.js")
    (include-js "/js/lib/react-dom-0.14.5.min.js")
    (include-js "/js/lib/react-bootstrap-0.28.1.min.js")
    (include-js "/js/lib/halogen.0.1.2.min.js")
    [:link {:rel "shortcut icon"
            :href "/img/favicon.ico"}]]
   [:body
    [:nav {:class "navbar navbar-default"}
     [:div {:class "container"}
      [:div {:class "nav-header"}
       [:a {:class "navbar-brand" :href "/"} "autotc"]]
      [:div {:class "collapse navbar-collapse"}
       [:p {:class "navbar-text navbar-right"}
        [:a {:class "navbar-link"
             :href "/settings"} "Settings"]]]]]
    [:div {:class "container"
           :id "main-content"} body]
    [:div {:class "github-fork-ribbon-wrapper right-bottom"}
     [:div {:class "github-fork-ribbon github-fork-custom-styles"}
      [:a {:href "https://github.com/yantonov/autotc"
           :target "_blank"}
       "Fork me on GitHub"]]]]))

(defn current-problems-file [problems]
  (html5
   [:head
    [:title "autotc"]
    (include-js "https://ajax.googleapis.com/ajax/libs/jquery/2.2.4/jquery.min.js")
    [:style nil
     (cljstr/join "\n"
                  ["<!--"
                   "body {"
                   "font-family: 'Helvetica Neue',Helvetica,Arial,sans-serif"
                   "}"
                   ""
                   "a, a:hover, a:visited {"
                   "color: #337ab7;"
                   "}"
                   "-->"])]]
   [:body

    [:div nil
     [:ol nil
      (map (fn [problem]
             [:li nil
              [:div nil
               [:a {:href ""
                    :class "test"}
                (:name problem)]
               [:div {:style "white-space: pre; display: none"
                      :class "stack"}
                (:details problem)]]])
           problems)]]
    [:script {:type "text/javascript"}
     (cljstr/join "\n"
                  ["$(document).ready(function () {"
                   "$('.test').click(function(e) {"
                   "$(e.target).parent().find('.stack').toggle();"
                   "return false;"
                   "});"
                   "})"])]]))
