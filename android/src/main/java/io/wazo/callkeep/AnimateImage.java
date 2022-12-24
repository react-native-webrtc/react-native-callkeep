package io.wazo.callkeep;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.DecelerateInterpolator;

import androidx.appcompat.widget.AppCompatImageView;

public class AnimateImage extends AppCompatImageView {

    private Animation mAnimation;

    public AnimateImage(Context context) {
        super(context);
    }

    public AnimateImage(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AnimateImage(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    // @Override
    // public void setOnClickListener(View.OnClickListener l) {
    //     super.setOnClickListener(getItemClickListener(l));
    // }

    private View.OnClickListener getItemClickListener(final View.OnClickListener listener) {
        return new View.OnClickListener() {

            @Override
            public void onClick(final View viewClicked) {
                mAnimation = getAnimation();
                if (mAnimation != null && !mAnimation.hasEnded()) {
                    return;
                }
                if (mAnimation == null) {
                    mAnimation = createItemDisappearAnimation(100, true);
                    mAnimation.setAnimationListener(new Animation.AnimationListener() {

                        @Override
                        public void onAnimationStart(Animation animation) {

                        }

                        @Override
                        public void onAnimationRepeat(Animation animation) {

                        }

                        @Override
                        public void onAnimationEnd(Animation animation) {
                            if (listener != null) {
                                listener.onClick(viewClicked);
                            }
                        }
                    });
                }
                startAnimation(mAnimation);
            }
        };
    }

    private Animation createItemDisappearAnimation(final long duration, final boolean isClicked) {
        AnimationSet animationSet = new AnimationSet(true);
        //animationSet.addAnimation(new ScaleAnimation(1.0f, isClicked ? 1.2f : 0.0f, 1.0f, isClicked ? 1.2f : 0.0f,
        //        Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f));
        animationSet.addAnimation(new AlphaAnimation(1.0f, 0.5f));

        animationSet.setDuration(duration);
        animationSet.setInterpolator(new DecelerateInterpolator());
        animationSet.setFillAfter(false);

        return animationSet;
    }

}
