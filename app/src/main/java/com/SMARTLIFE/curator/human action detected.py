import cv2
import imutils

# Load face detector
face_cascade = cv2.CascadeClassifier(cv2.data.haarcascades + 'haarcascade_frontalface_default.xml')

cap = cv2.VideoCapture(0)

print("Starting head tracking... Press 'q' to quit.")

prev_center = None
MOVEMENT_THRESHOLD = 20   # adjust sensitivity (increase = less sensitive)

while True:
    ret, frame = cap.read()
    if not ret:
        break

    frame = imutils.resize(frame, width=600)
    gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)

    faces = face_cascade.detectMultiScale(gray, 1.3, 5)

    current_center = None

    for (x, y, w, h) in faces:
        # Draw rectangle around face
        cv2.rectangle(frame, (x, y), (x+w, y+h), (0, 255, 0), 2)

        # Get center of face
        cx = x + w // 2
        cy = y + h // 2
        current_center = (cx, cy)

        # Draw center point
        cv2.circle(frame, current_center, 5, (0, 0, 255), -1)

        break  # only track first face

    # -------- Movement Logic --------
    if prev_center is not None and current_center is not None:
        dx = abs(current_center[0] - prev_center[0])
        dy = abs(current_center[1] - prev_center[1])

        if dx > MOVEMENT_THRESHOLD or dy > MOVEMENT_THRESHOLD:
            print("Human movement detected")

    prev_center = current_center

    cv2.imshow("Head Tracker", frame)

    if cv2.waitKey(1) & 0xFF == ord('q'):
        break

cap.release()
cv2.destroyAllWindows()