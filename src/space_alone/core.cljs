(ns space-alone.core
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [space-alone.macros :refer [with-context]])
  (:require [cljs.core.async :as a :refer [<! put! get! chan filter< map<]]
            [clojure.browser.dom :as dom :refer [log]]
            [goog.events :as events]
            [space-alone.constants :as c]
            [space-alone.utils :as u]))

;; TODO:
;; [ ] fix asteroids movement so that they don't fly away
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

(defn next-random
  [min max]
  (Math/floor (+ min (* (Math/random) (- max min -1)))))

(defn next-random-float
  [min max]
  (+ min (* (Math/random) (- max min))))

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

(defn init-screen
  []
  (let [screen-width  (.-innerWidth js/window)
        screen-height (.-innerHeight js/window)]
    (.setAttribute canvas "width" (str screen-width "px"))
    (.setAttribute canvas "height" (str screen-height "px"))
    (swap! state #(assoc % :screen-width screen-width :screen-height screen-height))))

(defn reset-state
  [{:keys [screen-width screen-height] :as state}]
  (merge state {:bullets       []
                :asteroids     []
                :next-asteroid (next-random c/MIN_TIME_BEFORE_ASTEROID c/MAX_TIME_BEFORE_ASTEROID)
                :ship          {:x          (/ screen-width 2)
                                :y          (/ screen-height 2)
                                :vX         0
                                :vY         0
                                :thrust     0
                                :rotation   0
                                :rotate     :none
                                :accelerate false
                                :shooting   false
                                :next-shoot 0}}))

(defn init-game
  []
  (swap! state reset-state))

(defn draw-ship
  [{:keys [x y rotation] :as ship}]
  (with-context [canvas ctx]
    (doto canvas
      (aset "strokeStyle" "blue")
      (aset "fillStyle" "blue")
      (.translate x y)
      (.rotate (* rotation c/RAD_FACTOR))
      (.beginPath)
      (.lineTo (/ c/SHIP_WIDTH -2) 0)
      (.lineTo 0 (- c/SHIP_HIGHT))
      (.lineTo (/ c/SHIP_WIDTH 2) 0)
      (.closePath)
      (.fill))))

(defn draw-bullet
  [{:keys [x y rotation]}]
  (with-context [canvas ctx]
    (doto canvas
      (.translate x y)
      (aset "strokeStyle" "#FFFFFF")
      (aset "lineWidth" 3)
      (.beginPath)
      (.moveTo -1 0)
      (.lineTo 1 0)
      (.stroke)
      (.closePath))))

(defn draw-asteroid
  [{:keys [x y size] :as asteroid}]
  (with-context [canvas ctx]
    (doto canvas
      (.translate x y)
      (aset "strokeStyle" "purple")
      (aset "fillStyle" "purple")
      (.beginPath)
      (.arc 0 0 (size c/ASTEROID_SIZES) 0 (* 2 Math/PI) false)
      (.fill)
      (.closePath))))

(defn draw-stage
  [{:keys [screen-width screen-height ship bullets asteroids] :as state}]
;;  (print bullets)
  (.clearRect ctx 0 0 screen-width screen-height)
  (doseq [b bullets]
    (draw-bullet b))
  (doseq [a asteroids]
    (draw-asteroid a))
  (draw-ship ship))

(defn next-x
  [screen-width x vX]
  (let [next-x (+ x vX)]
    (cond
     (> next-x screen-width) 0
     (neg? next-x)           screen-width
     :default                next-x)))

(defn next-y
  [screen-height y vY]
  (let [next-y (- y vY)]
    (cond
     (> next-y screen-height) 0
     (neg? next-y)           screen-height
     :default                next-y)))

(defn next-thrust
  [accelerate thrust]
  (if accelerate
    (min (+ thrust c/ACCELERATION) c/MAX_THRUST)
    (max 0 (- thrust c/THRUST_DECLINE))))

(defn next-velocity
  [vFn velocity accelerate rotation thrust]
  (if accelerate
    (let [next-velocity (+ velocity (* thrust (vFn (* rotation c/RAD_FACTOR))))]
      (min (max next-velocity (- c/MAX_VELOCITY)) c/MAX_VELOCITY))
    velocity))

(defn next-rotation
  [rotate rotation]
  (case rotate
    :left    (mod (- rotation c/TURN_FACTOR) 360)
    :right   (mod (+ rotation c/TURN_FACTOR) 360)
    rotation))

(defn update-bullet
  [{:keys [x y energy rotation]}]
  {:x        (- x (* c/BULLET_SPEED (Math/sin (* rotation (- c/RAD_FACTOR)))))
   :y        (- y (* c/BULLET_SPEED (Math/cos (* rotation (- c/RAD_FACTOR)))))
   :energy   (dec energy)
   :rotation rotation})

(defn update-asteroid
  [{:keys [x y vX vY] :as asteroid}]
  (merge asteroid {:x (+ x vX)
                   :y (+ y vY)}))


(defn create-asteroid
  ;; TODO: improve creation to make more random asteroid appearance
  [screen-width screen-height]
  (let [top?  (> 0.5 (Math/random))
        left? (> 0.5 (Math/random))
        vX    (next-random-float 0.01 0.5)
        vY    (next-random-float 0.01 0.5)]
    {:x    (if left? 0 screen-width)
     :y    (if top? 0 screen-height)
     :vX   (if left? vX (- vX))
     :vY   (if top?  vY (- vY))
     :power (:large c/ASTEROID_POWERS)
     :size :large}))

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
    :large (take 4 (repeatedly (fn []
                                 {:x    x
                                  :y    y
                                  :vX   (next-random-float 0.1 0.5)
                                  :vY   (next-random-float 0.1 0.5)
                                  :power (:medium c/ASTEROID_POWERS)
                                  :size :medium})))
    :medium (take 4 (repeatedly (fn []
                                  {:x    x
                                   :y    y
                                   :vX   (next-random-float 0.3 0.5)
                                   :vY   (next-random-float 0.3 0.5)
                                   :power (:small c/ASTEROID_POWERS)
                                   :size :small})))
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
      (let [{:keys [power] :as asteroid} (first asteroids)
            {:keys [hit bullets]}     (find-hit asteroid bullets)]
        (if-not (nil? hit)
          (let [power-left (- power (:energy hit))]
            (if (pos? power-left)
              (recur (rest asteroids) bullets (cons (assoc asteroid :power power-left) res))
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
  (swap! state (fn [{:keys [screen-width screen-height ship bullets asteroids next-asteroid] :as state}]
                 (let [{:keys [x y vX vY thrust rotation rotate accelerate shooting next-shoot]} ship
                       shoot?                                                                    (and shooting (zero? next-shoot))]
;;                   (println state)
                   ;; simplify all that by using a single merge?
                   (-> state
                       (merge {:asteroids     (if (zero? next-asteroid)
                                                (cons (create-asteroid screen-width screen-height) asteroids)
                                                asteroids)
                               :next-asteroid (if (zero? next-asteroid)
                                                (next-random c/MIN_TIME_BEFORE_ASTEROID c/MAX_TIME_BEFORE_ASTEROID)
                                                (max 0 (dec next-asteroid)))})
                       ;; handle asteroids movements
                       (update-in [:asteroids] #(map update-asteroid %))
                       ;; handle ship collisions with asteroids
                       (handle-collisions)
                       ;; handle shooting
                       (assoc :bullets (if shoot?
                                         (cons {:x x
                                                :y y
                                                :energy c/BULLET_ENERGY
                                                :rotation rotation}
                                               bullets)
                                         bullets))
                       ;; handle bullets movements
                       (update-in [:bullets] (fn [bullets]
                                               (->> bullets
                                                    (map update-bullet)
                                                    (filter #(pos? (:energy %))))))
                       (handle-bullet-hits)
                       ;; handle ship movements and handling
                       (update-in [:ship] (fn [ship]
                                            (merge ship {:x          (next-x screen-width x vX)
                                                         :y          (next-y screen-height y vY)
                                                         :vX         (next-velocity Math/sin vX accelerate rotation thrust)
                                                         :vY         (next-velocity Math/cos vY accelerate rotation thrust)
                                                         :rotation   (next-rotation rotate rotation)
                                                         :thrust     (next-thrust accelerate thrust)
                                                         :next-shoot (if shoot?
                                                                       c/TIME_BETWEEN_SHOOTS
                                                                       (max 0 (dec next-shoot)))}))))))))

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
            :start-shooting     (swap! state (ship-state-updater :shooting false true))
            :stop-shooting      (swap! state (ship-state-updater :shooting true false))
            nil)))))

(init-raf)
(init-screen)
(init-game)

(event-loop)

(draw-loop (.getTime (js/Date.)) (+ 10 (.getTime (js/Date.))) )
