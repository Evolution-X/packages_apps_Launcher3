/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.launcher3.allapps.search;

import static android.view.View.MeasureSpec.EXACTLY;
import static android.view.View.MeasureSpec.getSize;
import static android.view.View.MeasureSpec.makeMeasureSpec;

import static com.android.launcher3.Utilities.prefixTextWithIcon;
import static com.android.launcher3.icons.IconNormalizer.ICON_VISIBLE_AREA_FACTOR;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.graphics.Rect;
import android.net.Uri;
import android.text.Selection;
import android.text.SpannableStringBuilder;
import android.text.method.TextKeyListener;
import android.util.AttributeSet;
import android.view.ContextThemeWrapper;
import android.view.MotionEvent;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup.MarginLayoutParams;

import androidx.core.content.ContextCompat;

import com.android.launcher3.DeviceProfile;
import com.android.launcher3.ExtendedEditText;
import com.android.launcher3.Insettable;
import com.android.launcher3.R;
import com.android.launcher3.allapps.ActivityAllAppsContainerView;
import com.android.launcher3.allapps.AllAppsStore;
import com.android.launcher3.allapps.BaseAllAppsAdapter.AdapterItem;
import com.android.launcher3.LauncherPrefChangeListener;
import com.android.launcher3.LauncherPrefs;
import com.android.launcher3.allapps.PrivateProfileManager;
import com.android.launcher3.allapps.SearchUiManager;
import com.android.launcher3.search.SearchCallback;
import com.android.launcher3.util.Themes;
import com.android.launcher3.util.ApiWrapper;
import com.android.launcher3.Utilities;
import com.android.launcher3.views.ActivityContext;

import com.android.internal.util.android.Utils;

import java.util.ArrayList;

/**
 * Layout to contain the All-apps search UI.
 */
public class AppsSearchContainerLayout extends ExtendedEditText
        implements SearchUiManager, SearchCallback<AdapterItem>,
        AllAppsStore.OnUpdateListener, Insettable, LauncherPrefChangeListener {

    private final ActivityContext mLauncher;
    private final AllAppsSearchBarController mSearchBarController;
    private final SpannableStringBuilder mSearchQueryBuilder;

    private ActivityAllAppsContainerView<?> mAppsView;
    private int mHorizontalMargin = 0;

    // The amount of pixels to shift down and overlap with the rest of the content.
    private final int mContentOverlap;

    public AppsSearchContainerLayout(Context context) {
        this(context, null);
    }

    public AppsSearchContainerLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AppsSearchContainerLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        mLauncher = ActivityContext.lookupContext(context);
        mSearchBarController = new AllAppsSearchBarController();

        mSearchQueryBuilder = new SpannableStringBuilder();
        Selection.setSelection(mSearchQueryBuilder, 0);

        mContentOverlap =
                getResources().getDimensionPixelSize(R.dimen.all_apps_search_bar_content_overlap);
                
        mHorizontalMargin =
                getResources().getDimensionPixelSize(R.dimen.search_box_background_offset);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mAppsView.getAppsStore().addUpdateListener(this);
        LauncherPrefs.Companion.get(getContext()).addListener(this, LauncherPrefs.THEMED_ICONS);
        updateSearchBar();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        LauncherPrefs.Companion.get(getContext()).removeListener(this, LauncherPrefs.THEMED_ICONS);
        mAppsView.getAppsStore().removeUpdateListener(this);
    }

    @Override
    public void onPrefChanged(String key) {
        if ("themed_icons".equals(key)) {
            updateSearchBar();
        }
    }

    void updateSearchBar() {
        Context context = getContext();
        boolean isGsaInstalled = Utils.isPackageInstalled(
                context, Utilities.GSA_PACKAGE);

        int attrColor = Themes.getAttrColor(context, R.attr.qsbFillColor);
        int style = R.style.QsbIconTint;

        if (Themes.isThemedIconEnabled(context)) {
            attrColor = Themes.getAttrColor(context, R.attr.qsbFillColorThemedAllApps);
            style = R.style.QsbIconTint_Themed;
        }

        Context themedContext = new ContextThemeWrapper(context, style);

        if (getBackground() != null) getBackground().setTint(attrColor);

        int startDrawableRes = isGsaInstalled ? R.drawable.ic_super_g_color : R.drawable.ic_allapps_search;
        Drawable startDrawable = ContextCompat.getDrawable(themedContext, startDrawableRes);

        Drawable endDrawable = null;
        if (isGsaInstalled) {
            endDrawable = ContextCompat.getDrawable(themedContext, R.drawable.ic_lens_color);
        }

        setCompoundDrawablesRelativeWithIntrinsicBounds(startDrawable, null, endDrawable, null);
        
        if (isGsaInstalled) {
            setHint(null);
        } else {
            setHint(getContext().getString(R.string.all_apps_search_bar_hint));
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // Update the width to match the grid padding
        DeviceProfile dp = mLauncher.getDeviceProfile();
        int myRequestedWidth = getSize(widthMeasureSpec);
        int rowWidth = myRequestedWidth - mAppsView.getActiveRecyclerView().getPaddingLeft()
                - mAppsView.getActiveRecyclerView().getPaddingRight();

        int cellWidth = DeviceProfile.calculateCellWidth(rowWidth,
                dp.cellLayoutBorderSpacePx.x, dp.numShownHotseatIcons);
        int iconVisibleSize = Math.round(ICON_VISIBLE_AREA_FACTOR * dp.iconSizePx);
        int iconPadding = cellWidth - iconVisibleSize;

        int myWidth = rowWidth - iconPadding + getPaddingLeft() + getPaddingRight();
        super.onMeasure(makeMeasureSpec(myWidth, EXACTLY), heightMeasureSpec);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        // Shift the widget horizontally so that its centered in the parent (b/63428078)
        View parent = (View) getParent();
        int availableWidth = parent.getWidth() - parent.getPaddingLeft() - parent.getPaddingRight();
        int myWidth = right - left;
        int expectedLeft = parent.getPaddingLeft() + (availableWidth - myWidth) / 2;
        int shift = expectedLeft - left;
        setTranslationX(shift);

        offsetTopAndBottom(mContentOverlap);
    }

    @Override
    public void initializeSearch(ActivityAllAppsContainerView<?> appsView) {
        mAppsView = appsView;
        mSearchBarController.initialize(
                new DefaultAppSearchAlgorithm(getContext(), true),
                this, mLauncher, this);
    }

    @Override
    public void onAppsUpdated() {
        mSearchBarController.refreshSearchResult();
    }

    @Override
    public void resetSearch() {
        mSearchBarController.reset();
    }

    @Override
    public void focusSearchField() {
        mSearchBarController.focusSearchField();
    }

    @Override
    public void preDispatchKeyEvent(KeyEvent event) {
        // Determine if the key event was actual text, if so, focus the search bar and then dispatch
        // the key normally so that it can process this key event
        if (!mSearchBarController.isSearchFieldFocused() &&
                event.getAction() == KeyEvent.ACTION_DOWN) {
            final int unicodeChar = event.getUnicodeChar();
            final boolean isKeyNotWhitespace = unicodeChar > 0 &&
                    !Character.isWhitespace(unicodeChar) && !Character.isSpaceChar(unicodeChar);
            if (isKeyNotWhitespace) {
                boolean gotKey = TextKeyListener.getInstance().onKeyDown(this, mSearchQueryBuilder,
                        event.getKeyCode(), event);
                if (gotKey && mSearchQueryBuilder.length() > 0) {
                    mSearchBarController.focusSearchField();
                }
            }
        }
    }

    @Override
    public void onSearchResult(String query, ArrayList<AdapterItem> items) {
        if (query.equalsIgnoreCase(getContext().getString(R.string.private_space_label))) {
            privateSpaceQuery();
            return;
        }
        if (items != null) {
            mAppsView.setSearchResults(items);
        }
    }

    @Override
    public void clearSearchResult() {
        // Clear the search query
        mSearchQueryBuilder.clear();
        mSearchQueryBuilder.clearSpans();
        Selection.setSelection(mSearchQueryBuilder, 0);
        mAppsView.onClearSearchResult();
    }

    @Override
    public void setInsets(Rect insets) {
        MarginLayoutParams mlp = (MarginLayoutParams) getLayoutParams();
        mlp.topMargin = insets.top;
        mlp.leftMargin = mHorizontalMargin;
        mlp.rightMargin = mHorizontalMargin;
        requestLayout();
    }

    @Override
    public ExtendedEditText getEditText() {
        return this;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
            if (Utils.isPackageInstalled(getContext(), "rk.android.app.pixelsearch")) {
                if (event.getX() >= 0 && event.getX() <= getWidth() && event.getY() >= 0 && event.getY() <= getHeight()) {
                    launchPixelSearch();
                    return true;
                }
            }

            Drawable endDrawable = getCompoundDrawablesRelative()[2];
            if (endDrawable != null) {
                int iconStartX = getWidth() - getPaddingEnd() - endDrawable.getBounds().width();
                if (event.getX() >= iconStartX) {
                    launchLensSearch();
                    return true;
                }
            }

            Drawable startDrawable = getCompoundDrawablesRelative()[0];
            if (startDrawable != null) {
                int iconEndX = startDrawable.getBounds().width();
                if (event.getX() <= iconEndX) {
                    launchSearch();
                    return true;
                }
            }
        }
        return super.onTouchEvent(event);
    }

    private void launchLensSearch() {
        try {
            Intent lensIntent = new Intent(Intent.ACTION_VIEW);
            lensIntent.setComponent(new ComponentName(Utilities.GSA_PACKAGE, Utilities.LENS_ACTIVITY));
            lensIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            lensIntent.setData(Uri.parse(Utilities.LENS_URI));
            lensIntent.putExtra("LensHomescreenShortcut", true);
            getContext().startActivity(lensIntent);
        } catch (Exception e) {}
    }

    private void launchSearch() {
        boolean isGsaInstalled = Utils.isPackageInstalled(
                getContext(), Utilities.GSA_PACKAGE);
        try {
            Intent intent = new Intent();
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            
            if (isGsaInstalled) {
                intent.setAction("android.search.action.GLOBAL_SEARCH");
                intent.setPackage(Utilities.GSA_PACKAGE);
            } else {
                intent.setAction(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("https://www.google.com/search?q="));
            }
            getContext().startActivity(intent);
        } catch (Exception e) {}
    }

    private void launchPixelSearch() {
        try {
            Intent intent = new Intent();
            intent.setPackage("rk.android.app.pixelsearch");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            getContext().startActivity(intent);
        } catch (Exception e) {}
    }

    private void privateSpaceQuery() {
        PrivateProfileManager privateProfileManager = mAppsView.getPrivateProfileManager();
        if (privateProfileManager.isPrivateSpaceHidden()) {
            privateProfileManager.setQuietMode(false);
        } else if (!mAppsView.hasPrivateProfile()) {
            final Intent privateSpaceSettingsIntent =
                    ApiWrapper.INSTANCE.get(getContext()).getPrivateSpaceSettingsIntent();
            if (privateSpaceSettingsIntent != null) {
                mLauncher.startActivitySafely(mAppsView, privateSpaceSettingsIntent, null);
            }
        }
    }
}
