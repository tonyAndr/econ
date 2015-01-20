package org.osmdroid;

/**
 * Created by Tony on 20-Jan-15.
 */
public class Stage {
        public Integer number;
        public String start_point;
        public String end_point;

        public Stage(Integer number, String start_point, String end_point) {
            this.number = number;
            this.start_point = start_point;
            this.end_point = end_point;
        }
}
