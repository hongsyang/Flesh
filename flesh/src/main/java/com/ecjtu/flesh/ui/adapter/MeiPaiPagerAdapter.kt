package com.ecjtu.flesh.ui.adapter

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import android.support.v4.view.ViewPager
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.ecjtu.flesh.R
import com.ecjtu.flesh.model.models.MeiPaiModel
import com.ecjtu.flesh.model.models.V33Model
import com.ecjtu.netcore.model.MenuModel

/**
 * Created by Ethan_Xiang on 2018/2/9.
 */
class MeiPaiPagerAdapter(menu: List<MenuModel>, private val viewPager: ViewPager) : VideoTabPagerAdapter(menu, viewPager) {

    private val KEY_CARD_CACHE = "meipai_card_cache_"
    private val KEY_LAST_POSITION = "meipai_last_position_"
    private val KEY_LAST_POSITION_OFFSET = "meipai_last_position_offset_"

    private val mViewStub = HashMap<String, VH>()

    private var mMenuChildList: Map<String, List<MeiPaiModel>>? = null
    private var mLastScrolledPosition = 0


    init {
        viewPager.addOnPageChangeListener(this)
    }

    override fun onPageScrollStateChanged(state: Int) {
    }

    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
    }

    override fun onPageSelected(position: Int) {
        if (position != mLastScrolledPosition) {
            var recyclerView = mViewStub.get(getPageTitle(position))?.recyclerView
            recyclerView?.let {
                if (recyclerView?.adapter is IChangeTab) {
                    (recyclerView?.adapter as IChangeTab).onSelectTab()
                }
            }
            recyclerView = mViewStub.get(getPageTitle(mLastScrolledPosition))?.recyclerView
            recyclerView?.let {
                if (recyclerView?.adapter is IChangeTab) {
                    (recyclerView?.adapter as IChangeTab).onUnSelectTab()
                }
            }
        }
        mLastScrolledPosition = position
    }

    override fun isViewFromObject(view: View?, `object`: Any?): Boolean = view == `object`

    override fun getCount(): Int {
        return menu.size
    }

    override fun instantiateItem(container: ViewGroup?, position: Int): Any {
        Log.i("ttttttt", "VideoTabPagerAdapter instantiateItem " + position + " container " + container?.childCount)
        val item = LayoutInflater.from(container?.context).inflate(R.layout.layout_list_card_view, container, false)
        container?.addView(item)
        val title = getPageTitle(position) as String
        val vh = VH(item, menu[position], title)
        mMenuChildList?.get(title)?.let {
            vh.load(mMenuChildList!!.get(title)!!)
        }
        mViewStub.put(getPageTitle(position).toString(), vh)
        return item
    }

    override fun destroyItem(container: ViewGroup?, position: Int, `object`: Any?) {
        Log.i("ttttttt", "VideoTabPagerAdapter remove view " + position)
        container?.removeView(`object` as View)
        val vh: VH? = mViewStub.remove(getPageTitle(position))
        onDestroyItem(container?.context!!, getPageTitle(position).toString(), vh?.recyclerView, vh?.getPageModel())
        (vh?.recyclerView?.adapter as VideoCardListAdapter?)?.onRelease()
    }

    override fun getPageTitle(position: Int): CharSequence {
        return menu[position].title
    }

    override fun onDestroyItem(context: Context, key: String, recyclerView: RecyclerView?, pageModel: List<V33Model>?) {
//        thread {
//            val helper = PageListCacheHelper(context.filesDir.absolutePath)
//            if (pageModel != null) {
//                helper.put(KEY_CARD_CACHE + key, pageModel)
//            }
//        }
        val editor: SharedPreferences.Editor = PreferenceManager.getDefaultSharedPreferences(context).edit()
        if (recyclerView != null) {
            editor.putInt(KEY_LAST_POSITION + key, getScrollYPosition(recyclerView)).
                    putInt(KEY_LAST_POSITION_OFFSET + key, getScrollYOffset(recyclerView))
        }
        editor.apply()
    }

    override fun onStop(context: Context, tabIndex: Int, isExpand: Boolean) {
        val editor: SharedPreferences.Editor = PreferenceManager.getDefaultSharedPreferences(context).edit()
        for (entry in mViewStub) {
            val recyclerView = entry.value.recyclerView
            if (recyclerView != null) {
                editor.putInt(KEY_LAST_POSITION + entry.key,
                        getScrollYPosition(recyclerView)).
                        putInt(KEY_LAST_POSITION_OFFSET + entry.key, getScrollYOffset(recyclerView))
                if (recyclerView.adapter is VideoCardListAdapter) {
                    (recyclerView.adapter as VideoCardListAdapter).onStop()
                }
            }
        }
        if (tabIndex >= 0) {
            Log.i("tttttttttt", "videoTabPager " + tabIndex)
            editor.putInt(KEY_LAST_TAB_ITEM + "_" + VideoTabPagerAdapter::class.java.simpleName, tabIndex)
        }
        editor.putBoolean(KEY_APPBAR_LAYOUT_COLLAPSED, isExpand)
        editor.apply()
    }

    override fun onResume() {
        for (entry in mViewStub) {
            if (entry.value.recyclerView?.adapter is VideoCardListAdapter) {
                (entry.value.recyclerView?.adapter as VideoCardListAdapter).onResume()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        for (entry in mViewStub) {
            if (entry.value.recyclerView?.adapter is VideoCardListAdapter) {
                (entry.value.recyclerView?.adapter as VideoCardListAdapter).onDestroy()
            }
        }
        viewPager.removeOnPageChangeListener(this)
    }

    fun setMeiPaiList(mutableMap: MutableMap<String, List<MeiPaiModel>>) {
        mMenuChildList = mutableMap
    }

    override fun getViewStub(position: Int): View? {
        return mViewStub.get(menu[position].title)?.recyclerView
    }

    override fun getListSize(position: Int): Int {
        return mViewStub.get(menu[position].title)?.getPageModel()?.size ?: 0
    }

    private inner class VH(val itemView: View, private val menu: MenuModel, val key: String) {
        val recyclerView = itemView.findViewById(R.id.recycler_view) as RecyclerView?
        private var mPageModel: List<MeiPaiModel>? = null
        private val mRefreshLayout = if (itemView is SwipeRefreshLayout) itemView else null

        init {
            recyclerView?.layoutManager = LinearLayoutManager(recyclerView?.context, LinearLayoutManager.VERTICAL, false)
            initRefreshLayout()
        }

        fun load(v33ModelList: List<MeiPaiModel>) {
            mPageModel = v33ModelList
            loadCache(itemView.context, key)
        }

        fun getPageModel(): List<MeiPaiModel>? {
            return mPageModel
        }

        private fun loadCache(context: Context, key: String) {
            if (mPageModel != null) {
                recyclerView?.adapter = MeiPaiCardListAdapter(mPageModel!!, recyclerView!!)
                val lastPosition = PreferenceManager.getDefaultSharedPreferences(context).getInt(KEY_LAST_POSITION + key, -1)
                if (lastPosition >= 0) {
                    val yOffset = PreferenceManager.getDefaultSharedPreferences(context).getInt(KEY_LAST_POSITION_OFFSET + key, 0)
                    (recyclerView?.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(lastPosition, yOffset)
                }
            }
        }

        private fun initRefreshLayout() {
            if (mRefreshLayout != null) {
                mRefreshLayout.isEnabled = false
            }
        }
    }
}