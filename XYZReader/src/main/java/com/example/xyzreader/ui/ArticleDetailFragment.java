package com.example.xyzreader.ui;

import android.app.Fragment;
import android.app.LoaderManager;
import android.app.SharedElementCallback;
import android.content.Intent;
import android.content.Loader;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ShareCompat;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.format.DateUtils;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.util.List;
import java.util.Map;

/**
 * A fragment representing a single Article detail screen. This fragment is
 * either contained in a {@link ArticleListActivity} in two-pane mode (on
 * tablets) or a {@link ArticleDetailActivity} on handsets.
 */
public class ArticleDetailFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = "ArticleDetailFragment";

    public static final String ARG_ITEM_ID = "item_id";


    private Cursor mCursor;
    private long mItemId;
    private View mRootView;
    private int mMutedColor = 0xFF333333;
    private Toolbar toolbar;

    private ColorDrawable mStatusBarColorDrawable;

    private int mTopInset;

    private ImageView mPhotoView;
    private int mScrollY;
    private boolean mIsCard = false;
    private int mStatusBarFullOpacityBottom;
    private CollapsingToolbarLayout collapsingToolbar;
    private FloatingActionButton shareFab;
    private ImageView bluredView;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ArticleDetailFragment() {
    }

    public static ArticleDetailFragment newInstance(long itemId) {
        Bundle arguments = new Bundle();
        arguments.putLong(ARG_ITEM_ID, itemId);
        ArticleDetailFragment fragment = new ArticleDetailFragment();
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments().containsKey(ARG_ITEM_ID)) {
            mItemId = getArguments().getLong(ARG_ITEM_ID);
        }

        mIsCard = getResources().getBoolean(R.bool.detail_is_card);
        mStatusBarFullOpacityBottom = getResources().getDimensionPixelSize(
                R.dimen.detail_card_top_margin);
        setHasOptionsMenu(true);
    }

    public ArticleDetailActivity getActivityCast() {
        return (ArticleDetailActivity) getActivity();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // In support library r8, calling initLoader for a fragment in a FragmentPagerAdapter in
        // the fragment's onCreate may cause the same LoaderManager to be dealt to multiple
        // fragments because their mIndex is -1 (haven't been added to the activity yet). Thus,
        // we do this in onActivityCreated.
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_article_detail, container, false);
        collapsingToolbar = (CollapsingToolbarLayout) mRootView.findViewById(R.id.collapsing_toolbar);
        collapsingToolbar.setExpandedTitleColor(Color.argb(0, 0, 0, 0));


        mPhotoView = (ImageView) mRootView.findViewById(R.id.photo);
        //mPhotoContainerView = mRootView.findViewById(R.id.photo_container);

        mStatusBarColorDrawable = new ColorDrawable(0);

        mRootView.findViewById(R.id.share_fab).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(Intent.createChooser(ShareCompat.IntentBuilder.from(getActivity())
                        .setType("text/plain")
                        .setText("Some sample text")
                        .getIntent(), getString(R.string.action_share)));
            }
        });
        shareFab = (FloatingActionButton) mRootView.findViewById(R.id.share_fab);
        toolbar = (Toolbar) mRootView.findViewById(R.id.toolbar);
        bindViews();
        updateStatusBar();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            /*
            setEnterSharedElementCallback(new SharedElementCallback() {
                @Override
                public void onMapSharedElements(List<String> names, Map<String, View> sharedElements) {
                    sharedElements.put("artice_cover", mPhotoView);
                }
            });
            */
        }
        return mRootView;
    }

    private void updateStatusBar() {


        //mDrawInsetsFrameLayout.setInsetBackground(mStatusBarColorDrawable);
    }

    static float progress(float v, float min, float max) {
        return constrain((v - min) / (max - min), 0, 1);
    }

    static float constrain(float val, float min, float max) {
        if (val < min) {
            return min;
        } else if (val > max) {
            return max;
        } else {
            return val;
        }
    }

    private void bindViews() {
        if (mRootView == null) {
            return;
        }

        TextView titleView = (TextView) mRootView.findViewById(R.id.article_title);
        TextView bylineView = (TextView) mRootView.findViewById(R.id.article_byline);
        bylineView.setMovementMethod(new LinkMovementMethod());
        final TextView bodyView = (TextView) mRootView.findViewById(R.id.article_body);
        bluredView= (ImageView) mRootView.findViewById(R.id.blurred_photo);
        bodyView.setTypeface(Typeface.createFromAsset(getResources().getAssets(), "Rosario-Regular.ttf"));
        LinearLayout metabar = (LinearLayout) mRootView.findViewById(R.id.meta_bar);

        if (mCursor != null) {

            titleView.setText(mCursor.getString(ArticleLoader.Query.TITLE));
            collapsingToolbar.setTitle(mCursor.getString(ArticleLoader.Query.TITLE));
            bylineView.setText(Html.fromHtml(
                    DateUtils.getRelativeTimeSpanString(
                            mCursor.getLong(ArticleLoader.Query.PUBLISHED_DATE),
                            System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                            DateUtils.FORMAT_ABBREV_ALL).toString()
                            + " by <font color='#ffffff'>"
                            + mCursor.getString(ArticleLoader.Query.AUTHOR)
                            + "</font>"));
            bodyView.setText(Html.fromHtml(mCursor.getString(ArticleLoader.Query.BODY)));

            collapsingToolbar.invalidate();


            Picasso.with(getActivity())
                    .load(mCursor.getString(ArticleLoader.Query.PHOTO_URL))

                    .into(mPhotoView, new Callback() {
                        @Override
                        public void onSuccess() {
                            Bitmap bitmap = ((BitmapDrawable) mPhotoView.getDrawable()).getBitmap();
                            Palette p = Palette.from(bitmap).generate();
                            mMutedColor = p.getDarkMutedColor(0xFF333333);
                            mRootView.findViewById(R.id.meta_bar)
                                    .setBackgroundColor(mMutedColor);

                            mStatusBarColorDrawable.setColor(mMutedColor);
                            collapsingToolbar.setContentScrimColor(mMutedColor);
                            shareFab.setBackgroundTintList(ColorStateList.valueOf(mMutedColor));

                            bodyView.setLinkTextColor(p.getDarkVibrantColor(0xFF333333));
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                                AsyncTask<Bitmap,Void,Bitmap> task=new AsyncTask<Bitmap, Void, Bitmap>(){


                                    @Override
                                    protected Bitmap doInBackground(Bitmap... params) {


                                        Bitmap bitmap=Bitmap.createScaledBitmap(params[0],params[0].getWidth()/2, params[0].getHeight()/2, false);

                                        Bitmap bluredBitmap = Bitmap.createBitmap(
                                                bitmap.getWidth(), bitmap.getHeight(),
                                                Bitmap.Config.ARGB_8888);

                                        RenderScript renderScript = RenderScript.create(getActivity());
                                        ScriptIntrinsicBlur blur = ScriptIntrinsicBlur.create(renderScript,
                                                Element.U8_4(renderScript));


                                        Allocation blurInput = Allocation.createFromBitmap(renderScript, bitmap);
                                        Allocation blurOutput = Allocation.createFromBitmap(renderScript,bluredBitmap);

                                        blur.setInput(blurInput);

                                        blur.setRadius(25);
                                        blur.forEach(blurOutput);
                                        blur.setInput(blurOutput);
                                        blur.forEach(blurOutput);
                                        blur.setInput(blurOutput);
                                        blur.forEach(blurOutput);


                                        blurOutput.copyTo(bluredBitmap);
                                        renderScript.destroy();
                                        return  bluredBitmap;

                                    }

                                    @Override
                                    protected void onPostExecute(Bitmap bitmap) {
                                        bluredView.setImageBitmap(bitmap);
                                    }
                                };
                                task.execute(bitmap);


                            }


                            updateStatusBar();
                        }

                        @Override
                        public void onError() {

                        }
                    });

        } else {
            //mRootView.setVisibility(View.GONE);
            titleView.setText("N/A");
            collapsingToolbar.setTitle("N/A");
            bylineView.setText("N/A");
            bodyView.setText("N/A");
            metabar.invalidate();
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newInstanceForItemId(getActivity(), mItemId);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        if (!isAdded()) {
            if (cursor != null) {
                cursor.close();
            }
            return;
        }

        mCursor = cursor;
        if (mCursor != null && !mCursor.moveToFirst()) {
            Log.e(TAG, "Error reading item detail cursor");
            mCursor.close();
            mCursor = null;
        }

        bindViews();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mCursor = null;
        bindViews();
    }

    @Override
    public void onStart() {
        super.onStart();
        //getActivityCast().setSupportActionBar(toolbar);
        //  getActivityCast().getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }
}
