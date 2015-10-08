package douzifly.list

import android.animation.Animator
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import com.daimajia.swipe.SwipeLayout
import douzifly.list.model.Thing
import douzifly.list.model.ThingsManager
import douzifly.list.model.randomEmptyText
import douzifly.list.utils.*
import douzifly.list.widget.ColorPicker

public class MainActivity : AppCompatActivity() {

  companion object {
    val TAG = "MainActivity"
  }

  val mRecyclerView: RecyclerView by lazy {
    findViewById(R.id.recycler_view) as RecyclerView
  }

  val mFabButton: FloatingActionButton by lazy {
    findViewById(R.id.fab_add) as FloatingActionButton
  }

  val mInputPanel: View by lazy {
    findViewById(R.id.input_panel)
  }

  val mEditText: EditText by lazy {
    val ed = findViewById(R.id.edit_text) as EditText
    ed.typeface = fontSourceSansPro
    ed
  }

  val mRootView: View by lazy {
    findViewById(R.id.view_root)
  }

  val mTxtEmpty: View by lazy {
    findViewById(R.id.txt_empty)
  }

  val mTxtTitle: TextView by lazy {
    findViewById(R.id.txt_title) as TextView
  }

  val mColorPicker: ColorPicker by lazy {
    findViewById(R.id.color_picker) as ColorPicker
  }

  val mFabListener: (v: View) -> Unit = {

    if (mInputPanel.visibility == View.VISIBLE) {
      val cx = (mFabButton.left + mFabButton.right) / 2
      val cy = mFabButton.top + mFabButton.height / 2
      startCircularReveal(cx, cy, mInputPanel, true) {
        mInputPanel.visibility = View.INVISIBLE
      }
      setFabAsCommit(false)
      val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
      imm.hideSoftInputFromWindow(mEditText.windowToken, 0)

      handleInputDone()
      mEditText.setText("")
    } else {
      val cx = (mFabButton.left + mFabButton.right) / 2
      val cy = mFabButton.top + mFabButton.height / 2
      mInputPanel.visibility = View.VISIBLE
      startCircularReveal(cx, cy, mInputPanel, false) {
        mEditText.requestFocus()
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(mEditText, InputMethodManager.SHOW_FORCED)
      }
      setFabAsCommit(true)
    }
  }

  fun checkShowEmptyText() {
    mTxtEmpty.visibility = if (ThingsManager.things.size() == 0) View.VISIBLE else View.GONE
    if (mTxtEmpty.visibility == View.VISIBLE) {
      (mTxtEmpty as TextView).text = randomEmptyText()
    }
  }

  fun handleInputDone() {
    val textString = mEditText.text.toString().trim()
    if (textString.isBlank()) {
      return
    }

    ThingsManager.add(textString, -1, -1, mColorPicker.selectedColor)

  }

  fun setFabAsCommit(asCommit: Boolean) {
    if (asCommit) {
      mFabButton.setImageResource(R.drawable.ic_done_white)
      mFabButton.backgroundTintList = ColorStateList.valueOf(resources.getColor(R.color.fabDoneBackground))
      mFabButton.setRippleColor(resources.getColor(R.color.fabDoneRippleColor))
    } else {
      mFabButton.setImageResource(R.drawable.ic_add_white)
      mFabButton.setRippleColor(resources.getColor(R.color.fabAddRippleColor))
      mFabButton.backgroundTintList = ColorStateList.valueOf(resources.getColor(R.color.fabAddBackground))
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    mFabButton.setOnClickListener(mFabListener)
    mRecyclerView.layoutManager = LinearLayoutManager(this)
    mRecyclerView.adapter = ThingsAdapter()

    ThingsManager.onDataChanged = {
      checkShowEmptyText()
      (mRecyclerView.adapter as ThingsAdapter).things = ThingsManager.things
    }


    (mTxtEmpty as TextView).typeface = fontAlegreya
    mTxtTitle.typeface = fontRailway

    checkShowEmptyText()
  }

  override fun onDestroy() {
    super.onDestroy()
    ThingsManager.release()
  }

  fun startCircularReveal(cx: Int, cy: Int, viewRoot: View, reverse: Boolean, end: (() -> Unit)? = null) {
    val endRadius = Math.max(viewRoot.width, viewRoot.height).toFloat()
    val startRadius = if (reverse) endRadius else 0f
    val finalRadius = if (reverse) 0f else endRadius
    val anim = ViewAnimationUtils.createCircularReveal(viewRoot, cx, cy, startRadius, finalRadius)
    anim.setDuration(400)
    anim.addListener(object : Animator.AnimatorListener {
      override fun onAnimationRepeat(p0: Animator?) {
      }

      override fun onAnimationCancel(p0: Animator?) {
      }

      override fun onAnimationEnd(p0: Animator?) {
        end?.invoke()
        anim.removeListener(this)
      }

      override fun onAnimationStart(p0: Animator?) {
      }

    })
    anim.start()
  }

  override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
    if (keyCode == KeyEvent.KEYCODE_BACK) {
      if (mInputPanel.visibility == View.VISIBLE) {
        mFabListener.invoke(mFabButton)
        return false
      }
    }
    return super.onKeyDown(keyCode, event)
  }


  inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {

    override fun onClick(v: View?) {
      if (v == actionDelete) {
        doDelete()
      } else if (v == actionDone) {
        doDone()
      }

      swipeLayout.close(true)
    }

    fun doDelete() {
      if (thing == null) return
      "doDelete".logd(TAG)
      itemView.setBackgroundColor(resources.getColor(R.color.deleteRed))
      startCircularReveal(itemView.width, itemView.height, itemView, true) {
        ThingsManager.remove(thing!!)
      }
    }

    fun doDone() {
      if (thing == null) return
      "doDone".logd(TAG)
      ThingsManager.makeComplete(thing!!, !thing!!.isComplete)

    }

    var thing: Thing? = null

    val txtThing: TextView by lazy {
      itemView.findViewById(R.id.txt_thing) as TextView
    }

    val swipeLayout: SwipeLayout by lazy {
      itemView.findViewById(R.id.swipe_layout) as SwipeLayout
    }

    val actionViewWidth: Int by lazy {
      itemView.context.resources.getDimensionPixelSize(R.dimen.list_item_action_width)
    }

    val actionDelete: View by lazy {
      itemView.findViewById(R.id.action_delete)
    }

    val actionDone: View by lazy {
      itemView.findViewById(R.id.action_done)
    }

    val deleteLine: View by lazy {
      itemView.findViewById(R.id.delete_line)
    }

    fun bind(thing: Thing, prevThing: Thing?) {
      this.thing = thing
      updateUI(thing, prevThing)
      txtThing.text = thing.title
      txtThing.typeface = fontSourceSansPro
      swipeLayout.dragEdges = arrayListOf(SwipeLayout.DragEdge.Left, SwipeLayout.DragEdge.Right)
      actionDelete.setOnClickListener(this)
      actionDone.setOnClickListener(this)
    }

    fun updateUI(thing: Thing, prev: Thing?) {
      "updateUI vh:${this.hashCode()} ${thing.title} complete ${thing.isComplete}".logd(TAG)
      deleteLine.visibility = if (thing.isComplete) View.VISIBLE else View.GONE
      txtThing.setTextColor(resources.getColor(if (thing.isComplete) R.color.textDarkColor else R.color.textPrimaryColor))
      itemView.setBackgroundColor(if (thing.isComplete) Color.TRANSPARENT else makeThingColor(prev))
    }

    fun makeThingColor(prevThing: Thing?): Int {
      var color = thing!!.color

      if (prevThing?.color == color) {
        val newColor = ColorPicker.getDimedColor(color)
        thing?.color = newColor
        return newColor
      }

      return color
    }
  }

  inner class ThingsAdapter : RecyclerView.Adapter<VH>() {

    var things: List<Thing>? = null
      set(value: List<Thing>?) {
        $things = value
        notifyDataSetChanged()
      }

    override fun getItemCount(): Int {
      return things?.size() ?: 0
    }

    override fun onBindViewHolder(holder: VH?, position: Int) {
      val prev = if (position > 0 && position < itemCount) things?.get(position - 1) else null
      holder?.bind(things!!.get(position), prev)
    }

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): VH? {
      return VH(LayoutInflater.from(this@MainActivity).inflate(R.layout.thing_list_item, parent, false))
    }

  }
}
