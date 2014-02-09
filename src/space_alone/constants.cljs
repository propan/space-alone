(ns space-alone.constants)

(def RAD_FACTOR (/ Math.PI 180))

;; screen dimensions
(def SCREEN_WIDTH 1024)
(def SCREEN_HEIGHT 600)

(def LEFT_EDGE 5)
(def RIGHT_EDGE (- SCREEN_WIDTH LEFT_EDGE))

(def TOP_EDGE 5)
(def BOTTOM_EDGE (- SCREEN_HEIGHT TOP_EDGE))

;; ship dimensions
(def SHIP_WIDTH 14)
(def SHIP_HEIGHT 28)

;; movement constants
(def MAX_VELOCITY 6)
(def MAX_THRUST 2)

(def THRUST_DECLINE 0.3)

(def TURN_FACTOR 1.5)
(def ACCELERATION 0.01)

;; shooting constants
(def TIME_BETWEEN_SHOOTS 25)
(def BULLET_SPEED 15)
(def BULLET_ENERGY 35)

;; asteroids generation
(def MIN_TIME_BEFORE_ASTEROID 250)
(def MAX_TIME_BEFORE_ASTEROID 500)

(def ASTEROID_UNIT_SIZE 16)
(def ASTEROID_UNIT_REWARD 100)

;; controls
(def SPACE 32)

(def ARROW_LEFT 37)
(def ARROW_UP 38)
(def ARROW_RIGHT 39)
(def ARROW_DOWN 40)

;; graphics

(def SHADOW_BLUR 9)
