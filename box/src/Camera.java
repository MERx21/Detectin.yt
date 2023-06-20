import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

/**
 * Camera class for interacting with the camera and performing image processing tasks.
 */
class Camera {
    private VideoCapture capture;

    /**
     * Open the camera with the specified index.
     *
     * @param cameraIndex the index of the camera to open
     * @return true if the camera was successfully opened, false otherwise
     */
    public boolean open(int cameraIndex) {
        capture = new VideoCapture(cameraIndex);
        return capture.isOpened();
    }

    /**
     * Read a frame from the camera and store it in the provided Mat object.
     *
     * @param frame the Mat object to store the captured frame
     */
    public void read(Mat frame) {
        capture.read(frame);
    }

    /**
     * Release the camera resources.
     */
    public void release() {
        capture.release();
    }

    /**
     * Convert a Mat object to a BufferedImage.
     *
     * @param frame the Mat object to convert
     * @return the converted BufferedImage
     */
    public BufferedImage matToBufferedImage(Mat frame) {
        int width = frame.width();
        int height = frame.height();
        int channels = frame.channels();
        byte[] sourceData = new byte[width * height * channels];
        frame.get(0, 0, sourceData);

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
        final byte[] targetData = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        System.arraycopy(sourceData, 0, targetData, 0, sourceData.length);

        return image;
    }

    /**
     * Detect circles in the provided grayscale image using Hough transform.
     *
     * @param grayImage  the grayscale input image
     * @param threshold  the threshold value for circle detection
     * @param minRadius  the minimum radius of circles to be detected
     * @param maxRadius  the maximum radius of circles to be detected
     * @return a Mat object containing the detected circles
     */
    public Mat detectCircles(Mat grayImage, int threshold, int minRadius, int maxRadius) {
        Mat circles = new Mat();

        // Detect circles using Hough circle transform
        Imgproc.HoughCircles(
                grayImage,                  // Grayscale input image
                circles,                    // Output matrix to store detected circles
                Imgproc.HOUGH_GRADIENT,     // Detection method
                1,                          // Inverse ratio of accumulator resolution to image resolution
                grayImage.rows() / 12,       // Minimum distance between the centers of the detected circles
                100,                        // Higher threshold for Canny edge detection
                40,                         // Accumulator threshold for circle centers at the detection stage
                minRadius,                  // Minimum radius of circles to be detected
                maxRadius                   // Maximum radius of circles to be detected
        );

        return circles;
    }


    /**
     * Get the count of circles detected.
     *
     * @param circles the Mat object containing the detected circles
     * @return the count of circles detected
     */
    public int getCircleCount(Mat circles) {
        return circles.cols();
    }

    /**
     * Draw circles on the provided frame.
     *
     * @param frame    the frame to draw circles on
     * @param circles  the Mat object containing the circles
     * @param scale    the scaling factor for the circles' radius
     */
    public void drawCircles(Mat frame, Mat circles, double scale) {
        for (int i = 0; i < circles.cols(); i++) {
            double[] circle = circles.get(0, i);
            double centerX = circle[0];
            double centerY = circle[1];
            double radius = circle[2];

            Point center = new Point(centerX, centerY);
            // Draw a circle on the frame using the center and radius
            Imgproc.circle(frame, center, (int) (radius * scale), new Scalar(0, 255, 0), 4);
        }
    }
}
