package com.baidu.smartvideoplayer;

/**
 * @author machao10
 * @version v
 * @since 17/11/25
 */

public class Althorithm {
    int[] picture;
    int width;
    int height;

    public Althorithm() {
    }

    public Althorithm(int[] picture, int width, int height) {
        this.picture = picture;
        this.width = width;
        this.height = height;
    }

    public int[] getPicture() {
        return picture;
    }

    public void setPicture() {
    }
}

abstract class Decorator extends Althorithm {
    Althorithm althorithm;

    public void setAlthorithm(Althorithm althorithm) {
        this.althorithm = althorithm;
    }

    @Override
    public void setPicture() {
        if (althorithm != null) {
            althorithm.setPicture();
        }
    }
}

class ToGrey extends Decorator {
    @Override
    public void setPicture() {
        int[] grey = null;
        super.setPicture();
        PictureUtils.convertToGrey(picture, grey);
        althorithm.picture = grey;
    }
}

class Sobel extends Decorator {

    @Override
    public void setPicture() {
        int[] sobel = null;
        super.setPicture();
        PictureUtils.sobel(picture, width, height, sobel);
        althorithm.picture = sobel;
    }
}