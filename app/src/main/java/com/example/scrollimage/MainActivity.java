package com.example.scrollimage;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    ViewPager viewPager;
    List<View> lists;
    ConstraintLayout parent;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        viewPager = findViewById(R.id.vp);
        parent  = findViewById(R.id.parent);
        initItemView();
        viewPager.setAdapter(new MyAdapter());
        viewPager.setOffscreenPageLimit(3);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode ==RESULT_OK){
            Uri uri = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(),uri);
                ((PhotoView) lists.get(viewPager.getCurrentItem()).findViewById(R.id.image)).setImageBitmap(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0,0);
    }
    private void initItemView(){
        lists = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            View view = LayoutInflater.from(this).inflate(R.layout.item_photo,null);

            lists.add(view);
        }
    }
    class MyAdapter extends PagerAdapter{

        @Override
        public int getCount() {
            return 4;
        }

        @NonNull
        @Override
        public Object instantiateItem(@NonNull ViewGroup container, int position) {
            View view = lists.get(position);
            PhotoView imageView = view.findViewById(R.id.image);
            Button btn = view.findViewById(R.id.choose);
            imageView.setSingleDragListener(new PhotoView.OnSingleDragListener() {
                @Override
                public void onSingleDrag(float scaleValue, boolean isEnding) {

                    int alp ;
                    if (scaleValue >= 1){
                        alp = 255;
                    }else {
                        alp = (int) (255 * scaleValue);
                    }
                    parent.getBackground().setAlpha(alp);
                    if (isEnding){
                        finish();
                    }
                }
            });
            btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent albumIntent = new Intent(Intent.ACTION_PICK);
                    albumIntent.setData(MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    startActivityForResult(albumIntent,0);
                }
            });
            if (view.getParent() != null){
                ViewGroup vg  = (ViewGroup) view.getParent();
                vg.removeView(view);
            }
            container.addView(view);
            return view;
        }

        @Override
        public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {

        }

        @Override
        public boolean isViewFromObject(@NonNull View view, @NonNull Object o) {
            return view == o;
        }
    }
}

