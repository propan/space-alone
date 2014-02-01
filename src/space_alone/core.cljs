(ns space-alone.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :as a :refer [<! put! get! chan filter< map<]]
            [clojure.browser.dom :as dom]
            [goog.events :as events]
            [space-alone.constants :as c]
            [space-alone.draw :as d]
            [space-alone.models :as m]
            [space-alone.tick :as t]
            [space-alone.utils :as u]))

;; TODO:
;; [ ] make asteroids with shapes
;; [ ] accurate collision detection

(enable-console-print!)

(def controls-start
  {c/ARROW_LEFT  :start-rotate-left
   c/ARROW_RIGHT :start-rotate-right
   c/ARROW_UP    :start-accelerate
   c/SPACE       :start-shooting})

(def controls-stop
  {c/ARROW_LEFT  :stop-rotate-left
   c/ARROW_RIGHT :stop-rotate-right
   c/ARROW_UP    :stop-accelerate
   c/SPACE       :stop-shooting})

(def state (atom {}))

(def canvas (dom/get-element "open-space"))
(def ctx (.getContext canvas "2d"))

(defn listen
  [el type]
  (let [out (chan)]
    (events/listen el type
                   (fn [e] (put! out e)))
    out))

(defn raf-fn
  [last-time callback element]
  (let [current-time (.getTime (js/Date.))
        timeout      (Math/max 0 (- 16 (- current-time last-time)))
        next-call    (+ current-time timeout)]
    (.setTimeout js/window #(callback next-call) timeout)
    (aset js/window "requestAnimationFrame" (partial raf-fn next-call))))

(defn init-raf
  "Based on http://paulirish.com/2011/requestanimationframe-for-smart-animating"
  []
  (let [last-time 0]
    ;; resolve vendors rAF 
    (loop [afn     (.-requestAnimationFrame js/window)
           vendors ["ms" "webkit" "moz" "o"]]
      (when-not (or (not (nil? afn))
                    (empty? vendors))
        (let [vendor (first vendors)
              afn    (aget js/window (str vendor "RequestAnimationFrame"))]
          (println vendor)
          (aset js/window "requestAnimationFrame" afn)
          (aset js/window "cancelAnimationFrame" (or (aget js/window (str vendor "CancelAnimationFrame"))
                                                     (aget js/window (str vendor "CancelRequestAnimationFrame"))))
          (recur afn (rest vendors)))))
    ;; improvise, if it's not resolved
    (when-not (.-requestAnimationFrame js/window)
      (aset js/window "requestAnimationFrame" (partial raf-fn 0)))
    (when-not (.-cancelAnimationFrame js/window)
      (aset js/window "cancelAnimationFrame" js/clearTimeout))))

(defn reset-state
  [state]
  (merge state {:bullets       []
                :asteroids     []
                :next-asteroid (u/random-int c/MIN_TIME_BEFORE_ASTEROID c/MAX_TIME_BEFORE_ASTEROID)
                :ship          (m/ship (/ c/SCREEN_WIDTH 2)
                                       (/ c/SCREEN_HEIGHT 2))}))

(defn init-game
  []
  (swap! state reset-state))

(defn draw-stage
  [{:keys [ship bullets asteroids] :as state}]
  (.clearRect ctx 0 0 c/SCREEN_WIDTH c/SCREEN_HEIGHT)
  (doseq [b bullets]
    (d/draw b ctx))
  (doseq [a asteroids]
    (d/draw a ctx))
  (d/draw ship ctx))

(defn create-asteroid
  ;; TODO: improve creation to make more random asteroid appearance
  [screen-width screen-height]
  (let [top?  (> 0.5 (Math/random))
        left? (> 0.5 (Math/random))]
    (m/asteroid (if left? 0 screen-width)
                (if top? 0 screen-height)
                :large)))

(defn distance
  [x1 y1 x2 y2]
  (let [dx (- x2 x1)
        dy (- y2 y1)]
    (Math/sqrt (+ (* dx dx) (* dy dy)))))

(defn hit?
  [asteroid bullet]
  (let [size (:size asteroid)]
    (<= (distance (:x asteroid) (:y asteroid)
                  (:x bullet)   (:y bullet)) (size c/ASTEROID_SIZES))))

(defn find-hit
  [asteroid bullets]
  (loop [hit     nil
         res     []
         bullets bullets]
    (if (or (not (nil? hit)) 
            (empty? bullets))
      {:hit hit :bullets (concat res bullets)}
      (let [bullet (first bullets)]
        (if (hit? asteroid bullet)
          (recur bullet res (rest bullets))
          (recur nil (cons bullet res) (rest bullets)))))))

(defn break-asteroid
  [{:keys [x y vX vY size]}]
  (case size
    :large (take 4 (repeatedly #(m/asteroid x y :medium)))
    :medium (take 4 (repeatedly #(m/asteroid x y :small)))
    :small nil))

(defn handle-bullet-hits
  [{:keys [asteroids bullets] :as state}]
  (loop [asteroids asteroids
         bullets   bullets
         res       []]
    (if (or (empty? asteroids)
            (empty? bullets))
      (merge state {:asteroids (concat res asteroids)
                    :bullets   bullets})
      (let [{:keys [energy] :as asteroid} (first asteroids)
            {:keys [hit bullets]}     (find-hit asteroid bullets)]
        (if-not (nil? hit)
          (let [energy-left (- energy (:energy hit))]
            (if (pos? energy-left)
              (recur (rest asteroids) bullets (cons (assoc asteroid :energy energy-left) res))
              (recur (rest asteroids) bullets (concat res (break-asteroid asteroid)))))
          (recur (rest asteroids) bullets (cons asteroid res)))))))

(defn collide?
  [ship asteroid]
  (< (distance (:x ship) (:y ship)
               (:x asteroid) (:y asteroid))
     ((:size asteroid) c/ASTEROID_SIZES)))

(defn handle-collisions
  [{:keys [ship asteroids] :as state}]
  (let [collisions (filter (partial collide? ship) asteroids)]
    (if (empty? collisions)
      state
      (reset-state state))))

(defn update-stage
  []
  (swap! state (fn [{:keys [ship bullets asteroids next-asteroid] :as state}]
                 (let [screen-width                                  c/SCREEN_WIDTH
                       screen-height                                 c/SCREEN_HEIGHT
                       {:keys [x y vX vY rotation shoot next-shoot]} ship
                       shoot?                                        (and shoot (zero? next-shoot))]
;;                   (println state)
                   ;; simplify all that by using a single merge?
                   (-> state
                       (merge {:asteroids     (if (zero? next-asteroid)
                                                (cons (create-asteroid screen-width screen-height) asteroids)
                                                asteroids)
                               :next-asteroid (if (zero? next-asteroid)
                                                (u/random-int c/MIN_TIME_BEFORE_ASTEROID c/MAX_TIME_BEFORE_ASTEROID)
                                                (max 0 (dec next-asteroid)))})
                       ;; handle asteroids movements
                       (update-in [:asteroids] #(map t/tick %))
                       ;; handle ship collisions with asteroids
                       (handle-collisions)
                       ;; handle shooting
                       (assoc :bullets (if shoot?
                                         (cons (m/bullet x y rotation)
                                               bullets)
                                         bullets))
                       ;; handle bullets movements
                       (update-in [:bullets] (fn [bullets]
                                               (->> bullets
                                                    (map t/tick)
                                                    (filter #(pos? (:energy %))))))
                       (handle-bullet-hits)
                       ;; handle ship movements and handling
                       (update-in [:ship] t/tick))))))

(defn draw-loop
  [then time]
  (let [delta (- time then)
        fps   (.toFixed (/ 1000 delta) 1) ]
    (update-stage)
    (draw-stage @state)
    (.requestAnimationFrame js/window (partial draw-loop time))))

(defn- to-event
  [mapping]
  (fn [e]
    (get mapping (.-keyCode e) :unknown)))

(defn ship-state-updater
  [p from to]
  (fn [state]
    (update-in state [:ship p] #(if (= % from) to %))))

(defn event-loop
  []
  (let [movements (filter< #(not (= :unknown %))
                           (a/merge [(map< (to-event controls-start) (listen js/document events/EventType.KEYDOWN))
                                     (map< (to-event controls-stop) (listen js/document events/EventType.KEYUP))]))
        clicks   (a/merge [movements]) ]
    (go (while true
          (case (<! movements)
            :start-rotate-left  (swap! state (ship-state-updater :rotate :none :left))
            :stop-rotate-left   (swap! state (ship-state-updater :rotate :left :none))
            :start-rotate-right (swap! state (ship-state-updater :rotate :none :right))
            :stop-rotate-right  (swap! state (ship-state-updater :rotate :right :none))

            :start-accelerate   (swap! state (ship-state-updater :accelerate false true))
            :stop-accelerate    (swap! state (ship-state-updater :accelerate true false))
            :start-shooting     (swap! state (ship-state-updater :shoot false true))
            :stop-shooting      (swap! state (ship-state-updater :shoot true false))
            nil)))))

(init-raf)
(init-game)

(event-loop)

(draw-loop (.getTime (js/Date.)) (+ 10 (.getTime (js/Date.))) )
