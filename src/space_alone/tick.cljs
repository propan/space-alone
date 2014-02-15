(ns space-alone.tick
  (:require [space-alone.constants :as C]
            [space-alone.models :as m :refer [Asteroid Bullet ObjectPiece Particle Ship TextEffect
                                              GameScreen GameOverScreen WelcomeScreen]]
            [space-alone.utils :as u]))

;;
;; Movement Functions
;;
(defn- next-position
  [position dFn velocity max-position]
  (let [next (dFn position velocity)]
    (cond
     (>= next max-position) 0
     (< next 0)             (- max-position 1)
     :default               next)))

(defn- next-rotation
  [rotate rotation turn-factor]
  (case rotate
    :left    (mod (- rotation turn-factor) 360)
    :right   (mod (+ rotation turn-factor) 360)
    rotation))

(defn- next-thrust
  [accelerate thrust]
  (if accelerate
    (min (+ thrust C/ACCELERATION) C/MAX_THRUST)
    (max 0 (- thrust C/THRUST_DECLINE))))

(defn- next-velocity
  [vFn velocity accelerate rotation thrust]
  (if accelerate
    (let [next-velocity (+ velocity (* thrust (vFn (* rotation C/RAD_FACTOR))))]
      (min (max next-velocity (- C/MAX_VELOCITY)) C/MAX_VELOCITY))
    velocity))

;;
;; Tickable Protocol
;;

(defprotocol Tickable
  (tick [_]))

(extend-type Asteroid
  Tickable
  (tick [{:keys [x y vX vY rotate rotation rotation-speed] :as asteroid}]
    (merge asteroid {:x        (next-position x + vX C/SCREEN_WIDTH)
                     :y        (next-position y + vY C/SCREEN_HEIGHT)
                     :rotation (next-rotation rotate rotation rotation-speed)})))

(extend-type Bullet
  Tickable
  (tick [{:keys [x y vX vY energy] :as bullet}]
    (merge bullet {:x      (next-position x - vX C/SCREEN_WIDTH)
                   :y      (next-position y - vY C/SCREEN_HEIGHT)
                   :energy (dec energy)})))

(extend-type ObjectPiece
  Tickable
  (tick [{:keys [x y vX vY rotate rotation rotation-speed ticks-left] :as asteroid-piece}]
    (merge asteroid-piece {:x          (next-position x + vX C/SCREEN_WIDTH)
                           :y          (next-position y + vY C/SCREEN_HEIGHT)
                           :rotation   (next-rotation rotate rotation rotation-speed)
                           :ticks-left (dec ticks-left)})))

(extend-type Particle
  Tickable
  (tick [{:keys [x y vX vY ticks-left] :as particle}]
    (merge particle {:x          (next-position x - vX C/SCREEN_WIDTH)
                     :y          (next-position y - vY C/SCREEN_HEIGHT)
                     :ticks-left (dec ticks-left)})))

(extend-type Ship
  Tickable
  (tick [{:keys [x y vX vY rotation thrust accelerate rotate shoot next-shoot] :as ship}]
    (let [shoot? (and shoot (zero? next-shoot))]
      (merge ship {:x          (next-position x + vX C/SCREEN_WIDTH)
                   :y          (next-position y - vY C/SCREEN_HEIGHT)
                   :vX         (next-velocity Math/sin vX accelerate rotation thrust)
                   :vY         (next-velocity Math/cos vY accelerate rotation thrust)
                   :rotation   (next-rotation rotate rotation C/TURN_FACTOR)
                   :thrust     (next-thrust accelerate thrust)
                   :next-shoot (if shoot?
                                 C/TIME_BETWEEN_SHOOTS
                                 (max 0 (dec next-shoot)))}))))

(extend-type TextEffect
  Tickable
  (tick [{:keys [scale ticks-left] :as effect}]
    (merge effect {:scale      (+ scale 0.05)
                   :ticks-left (dec ticks-left)})))

;;
;; Effects
;;

(defn create-hit-effect
  [{:keys [x y]}]
  (repeatedly (u/random-int 4 12) #(m/particle x y)))

(defn create-asteroid-break-effect
  [{:keys [x y type size rotation] :as a} reward]
  (let [points (get C/ASTEROID_POINTS type)
        pieces (partition 2 1 (take 1 points) points)]
    (cons
     (m/text-effect (str reward) x y)
     (map (fn [[[lx ly] [rx ry]]]
            (m/asteroid-piece x y lx ly rx ry size rotation)) pieces))))

(defn create-ship-explosion-effect
  [{:keys [x y vX vY rotation] :as ship}]
  (let [points [[-10 10] [0 -15] [10 10] [7 5] [-7 5]]
        pieces (partition 2 1 (take 1 points) points)]
    (concat
     (create-hit-effect ship)
     (map (fn [[[lx ly] [rx ry]]]
            (m/ship-piece x y lx ly rx ry vX vY rotation)) pieces))))

;;
;; Game Screen
;;

(defn create-asteroid
  [screen-width screen-height]
  (let [side (Math/random)]
    (cond
     (<= side 0.25) (m/asteroid 0 (u/random-int 0 screen-height) 4)
     (<= side 0.50) (m/asteroid screen-width (u/random-int 0 screen-height) 4)
     (<= side 0.75) (m/asteroid (u/random-int 0 screen-width) 0 4)
     :else          (m/asteroid (u/random-int 0 screen-width) screen-width 4))))

(defn break-asteroid
  [{:keys [x y vX vY size]}]
  (case size
    4 (take 4 (repeatedly #(m/asteroid x y 3)))
    3 (take 4 (repeatedly #(m/asteroid x y 2)))
    2 (take 4 (repeatedly #(m/asteroid x y 1)))
    1 nil))

(defn hit?
  [o asteroid]
  (<= (u/distance (:x o) (:y o)
                  (:x asteroid) (:y asteroid))
      (+ (* (:size asteroid) C/ASTEROID_UNIT_SIZE) (:radius o))))

(defn- asteroids-tick
  [asteroids add-asteroid?]
  (->> (if add-asteroid?
         (cons (create-asteroid C/SCREEN_WIDTH C/SCREEN_HEIGHT) asteroids)
         asteroids)
       (map tick)))

(defn- bullets-tick
  [bullets shoot? ship-x ship-y ship-rotation]
  (->> (if shoot?
         (cons (m/bullet ship-x ship-y ship-rotation) bullets)
         bullets)
       (map tick)
       (filter #(pos? (:energy %)))))

(defn- effects-tick
  [effects]
  (->> effects
       (map tick effects)
       (filter #(pos? (:ticks-left %)))))

(defn find-hit
  [asteroid bullets]
  (loop [hit     nil
         res     []
         bullets bullets]
    (if (or (not (nil? hit))
            (empty? bullets))
      {:hit hit :bullets (into res bullets)}
      (let [bullet (first bullets)]
        (if (hit? bullet asteroid)
          (recur bullet res (rest bullets))
          (recur nil (conj res bullet) (rest bullets)))))))

(defn handle-bullet-hits
  [{:keys [asteroids bullets effects score] :as state}]
  (loop [asteroids   asteroids
         bullets     bullets
         res         []
         new-effects []
         points      score]
    (if (or (empty? asteroids)
            (empty? bullets))
      (merge state {:asteroids (into res asteroids)
                    :bullets   bullets
                    :effects   (concat new-effects effects)
                    :score     points})
      (let [{:keys [energy size] :as asteroid} (first asteroids)
            {:keys [hit bullets]}              (find-hit asteroid bullets)]
        (if-not (nil? hit)
          (let [energy-left (- energy (:energy hit))]
            (if (pos? energy-left)
              (recur (rest asteroids) bullets
                     (conj res (assoc asteroid :energy energy-left))
                     (into new-effects (create-hit-effect hit))
                     points)
              (let [reward (* size C/ASTEROID_UNIT_REWARD)]
                (recur (rest asteroids) bullets
                       (into res (break-asteroid asteroid))
                       (into new-effects (create-asteroid-break-effect asteroid reward))
                       (+ points reward)))))
          (recur (rest asteroids) bullets (conj res asteroid) new-effects points))))))

(defn detect-collision
  [{:keys [ship asteroids lives] :as state}]
  (if (some #(hit? ship %) asteroids)
    (let [lives (dec lives)]
      (cond-> (update-in state [:effects] concat (create-ship-explosion-effect ship))
              (pos? lives) (merge {:lives lives
                                   :ship  (m/ship (/ C/SCREEN_WIDTH 2)
                                                  (/ C/SCREEN_HEIGHT 2))})
              (<= lives 0) (m/game-over-screen)))
    state))

(extend-type GameScreen
  Tickable
  (tick [{:keys [ship bullets asteroids effects next-asteroid] :as state}]
    (let [{:keys [x y rotation shoot next-shoot]} ship]
      (-> state
          (merge {:asteroids     (asteroids-tick asteroids (zero? next-asteroid))
                  :bullets       (bullets-tick bullets (and shoot (zero? next-shoot)) x y rotation)
                  :effects       (effects-tick effects)
                  :ship          (tick ship)
                  :next-asteroid (if (zero? next-asteroid)
                                   (u/random-int C/MIN_TIME_BEFORE_ASTEROID C/MAX_TIME_BEFORE_ASTEROID)
                                   (max 0 (dec next-asteroid)))})
          (handle-bullet-hits)
          (detect-collision)))))

(extend-type GameOverScreen
  Tickable
  (tick [{:keys [bullets asteroids effects] :as state}]
    (-> state
        (merge {:asteroids (asteroids-tick asteroids false)
                :bullets   (bullets-tick bullets false 0 0 0)
                :effects   (effects-tick effects)})
        (handle-bullet-hits))))

(extend-type WelcomeScreen
  Tickable
  (tick [state]
    (update-in state [:asteroids] #(map tick %))))
