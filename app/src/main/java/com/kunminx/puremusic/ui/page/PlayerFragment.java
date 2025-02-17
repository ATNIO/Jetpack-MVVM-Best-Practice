/*
 * Copyright 2018-present KunMinX
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kunminx.puremusic.ui.page;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.SeekBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModel;

import com.kunminx.architecture.ui.page.BaseFragment;
import com.kunminx.architecture.ui.page.DataBindingConfig;
import com.kunminx.architecture.ui.page.State;
import com.kunminx.architecture.utils.ToastUtils;
import com.kunminx.architecture.utils.Utils;
import com.kunminx.player.PlayingInfoManager;
import com.kunminx.puremusic.BR;
import com.kunminx.puremusic.R;
import com.kunminx.puremusic.databinding.FragmentPlayerBinding;
import com.kunminx.puremusic.domain.message.DrawerCoordinateManager;
import com.kunminx.puremusic.domain.event.Messages;
import com.kunminx.puremusic.domain.message.PageMessenger;
import com.kunminx.puremusic.player.PlayerManager;
import com.kunminx.puremusic.ui.page.helper.DefaultInterface;
import com.kunminx.puremusic.ui.view.PlayerSlideListener;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import net.steamcrafted.materialiconlib.MaterialDrawableBuilder;

/**
 * Create by KunMinX at 19/10/29
 */
public class PlayerFragment extends BaseFragment {

    //TODO tip 1：基于 "单一职责原则"，应将 ViewModel 划分为 state-ViewModel 和 event-ViewModel，
    // state-ViewModel 职责仅限于托管、保存和恢复本页面 state，
    // event-ViewModel 职责仅限于 "消息分发" 场景承担 "唯一可信源"。

    // 如这么说无体会，详见 https://xiaozhuanlan.com/topic/8204519736

    private PlayerViewModel mStates;
    private PageMessenger mMessenger;
    private PlayerSlideListener mListener;

    @Override
    protected void initViewModel() {
        mStates = getFragmentScopeViewModel(PlayerViewModel.class);
        mMessenger = getApplicationScopeViewModel(PageMessenger.class);
    }

    @Override
    protected DataBindingConfig getDataBindingConfig() {

        //TODO tip 2: DataBinding 严格模式：
        // 将 DataBinding 实例限制于 base 页面中，默认不向子类暴露，
        // 通过这方式，彻底解决 View 实例 Null 安全一致性问题，
        // 如此，View 实例 Null 安全性将和基于函数式编程思想的 Jetpack Compose 持平。
        // 而 DataBindingConfig 就是在这样背景下，用于为 base 页面 DataBinding 提供绑定项。

        // 如这么说无体会，详见 https://xiaozhuanlan.com/topic/9816742350 和 https://xiaozhuanlan.com/topic/2356748910

        return new DataBindingConfig(R.layout.fragment_player, BR.vm, mStates)
            .addBindingParam(BR.click, new ClickProxy())
            .addBindingParam(BR.listener, new ListenerHandler());
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mMessenger.output(this, messages -> {
            switch (messages.eventId) {
                case Messages.EVENT_ADD_SLIDE_LISTENER:
                    if (view.getParent().getParent() instanceof SlidingUpPanelLayout) {
                        SlidingUpPanelLayout sliding = (SlidingUpPanelLayout) view.getParent().getParent();
                        sliding.addPanelSlideListener(mListener = new PlayerSlideListener((FragmentPlayerBinding) getBinding(), sliding));
                        sliding.addPanelSlideListener(new DefaultInterface.PanelSlideListener() {
                            @Override
                            public void onPanelStateChanged(
                                View view, SlidingUpPanelLayout.PanelState panelState,
                                SlidingUpPanelLayout.PanelState panelState1) {
                                DrawerCoordinateManager.getInstance().requestToUpdateDrawerMode(
                                    panelState1 == SlidingUpPanelLayout.PanelState.EXPANDED,
                                    this.getClass().getSimpleName()
                                );
                            }
                        });
                    }
                    break;
                case Messages.EVENT_CLOSE_SLIDE_PANEL_IF_EXPANDED:
                    // 按下返回键，如果此时 slide 面板是展开的，那么只对面板进行 slide down

                    if (view.getParent().getParent() instanceof SlidingUpPanelLayout) {
                        SlidingUpPanelLayout sliding = (SlidingUpPanelLayout) view.getParent().getParent();
                        if (sliding.getPanelState() == SlidingUpPanelLayout.PanelState.EXPANDED) {
                            sliding.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
                        } else {

                            // TODO tip 4：此处演示向 "唯一可信源" 发送请求，以便实现 "生命周期安全、消息分发可靠一致" 的通知。

                            // 如这么说无体会，详见 https://xiaozhuanlan.com/topic/0168753249
                            // --------
                            // 与此同时，此处传达的另一思想是 "最少知道原则"，
                            // Activity 内部事情在 Activity 内部消化，不要试图在 fragment 中调用和操纵 Activity 内部东西。
                            // 因为 Activity 端的处理后续可能会改变，且可受用于更多 fragment，而不单单是本 fragment。

                            // TODO: yes:

                            mMessenger.input(new Messages(Messages.EVENT_CLOSE_ACTIVITY_IF_ALLOWED));

                            // TODO: do not:
                            // mActivity.finish();
                        }
                    } else {
                        mMessenger.input(new Messages(Messages.EVENT_CLOSE_ACTIVITY_IF_ALLOWED));
                    }
                    break;
            }
        });

        // TODO tip 3：所有播放状态的改变，皆来自 "唯一可信源" PlayerManager 统一分发，
        //  如此才能确保 "消息分发可靠一致"，避免不可预期的推送和错误。

        // 如这么说无体会，详见 https://xiaozhuanlan.com/topic/0168753249

        PlayerManager.getInstance().getChangeMusicResult().observe(getViewLifecycleOwner(), changeMusic -> {

            // 切歌时，音乐的标题、作者、封面 状态的改变
            mStates.title.set(changeMusic.getTitle());
            mStates.artist.set(changeMusic.getSummary());
            mStates.coverImg.set(changeMusic.getImg());

            if (mListener != null) {
                view.post(mListener::calculateTitleAndArtist);
            }
        });

        PlayerManager.getInstance().getPlayingMusicResult().observe(getViewLifecycleOwner(), playingMusic -> {

            // 播放进度 状态的改变
            mStates.maxSeekDuration.set(playingMusic.getDuration());
            mStates.currentSeekPosition.set(playingMusic.getPlayerPosition());
        });

        PlayerManager.getInstance().getPauseResult().observe(getViewLifecycleOwner(), aBoolean -> {

            // 播放按钮 状态的改变
            mStates.isPlaying.set(!aBoolean);
        });

        PlayerManager.getInstance().getPlayModeResult().observe(getViewLifecycleOwner(), anEnum -> {
            int tip;
            if (anEnum == PlayingInfoManager.RepeatMode.LIST_CYCLE) {
                mStates.playModeIcon.set(MaterialDrawableBuilder.IconValue.REPEAT);
                tip = R.string.play_repeat;
            } else if (anEnum == PlayingInfoManager.RepeatMode.SINGLE_CYCLE) {
                mStates.playModeIcon.set(MaterialDrawableBuilder.IconValue.REPEAT_ONCE);
                tip = R.string.play_repeat_once;
            } else {
                mStates.playModeIcon.set(MaterialDrawableBuilder.IconValue.SHUFFLE);
                tip = R.string.play_shuffle;
            }
            if (view.getParent().getParent() instanceof SlidingUpPanelLayout) {
                SlidingUpPanelLayout sliding = (SlidingUpPanelLayout) view.getParent().getParent();
                if (sliding.getPanelState() == SlidingUpPanelLayout.PanelState.EXPANDED) {
                    ToastUtils.showShortToast(getApplicationContext(), getString(tip));
                }
            }
        });
    }

    // TODO tip 5：此处通过 DataBinding 规避 setOnClickListener 时存在的 View 实例 Null 安全一致性问题，

    // 也即，有视图就绑定，无就无绑定，总之 不会因不一致性造成 View 实例 Null 安全问题。
    // 如这么说无体会，详见 https://xiaozhuanlan.com/topic/9816742350

    public class ClickProxy {

        public void playMode() {
            PlayerManager.getInstance().changeMode();
        }

        public void previous() {
            PlayerManager.getInstance().playPrevious();
        }

        public void togglePlay() {
            PlayerManager.getInstance().togglePlay();
        }

        public void next() {
            PlayerManager.getInstance().playNext();
        }

        public void showPlayList() {
            ToastUtils.showShortToast(getApplicationContext(), getString(R.string.unfinished));
        }

        public void slideDown() {
            mMessenger.input(new Messages(Messages.EVENT_CLOSE_SLIDE_PANEL_IF_EXPANDED));
        }

        public void more() {
        }
    }

    public static class ListenerHandler implements DefaultInterface.OnSeekBarChangeListener {

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            PlayerManager.getInstance().setSeek(seekBar.getProgress());
        }
    }

    //TODO tip 6：每个页面都需单独准备一个 state-ViewModel，托管 DataBinding 绑定的 State，
    // 此外，state-ViewModel 职责仅限于状态托管和保存恢复，不建议在此处理 UI 逻辑，
    // UI 逻辑只适合在 Activity/Fragment 等视图控制器中完成，是 “数据驱动” 一部分，将来升级到 Jetpack Compose 更是如此。

    //如这么说无体会，详见 https://xiaozhuanlan.com/topic/9816742350

    public static class PlayerViewModel extends ViewModel {

        //TODO tip 10：此处我们使用 "去除防抖特性" 的 ObservableField 子类 State，用以代替 MutableLiveData，

        //如这么说无体会，详见 https://xiaozhuanlan.com/topic/9816742350

        public final State<String> title = new State<>(Utils.getApp().getString(R.string.app_name));

        public final State<String> artist = new State<>(Utils.getApp().getString(R.string.app_name));

        public final State<String> coverImg = new State<>("");

        public final State<Drawable> placeHolder = new State<>(ContextCompat.getDrawable(Utils.getApp(), R.drawable.bg_album_default));

        public final State<Integer> maxSeekDuration = new State<>(0);

        public final State<Integer> currentSeekPosition = new State<>(0);

        public final State<Boolean> isPlaying = new State<>(false, false);

        public final State<MaterialDrawableBuilder.IconValue> playModeIcon = new State<>(MaterialDrawableBuilder.IconValue.REPEAT);

        {
            if (PlayerManager.getInstance().getRepeatMode() == PlayingInfoManager.RepeatMode.LIST_CYCLE) {
                playModeIcon.set(MaterialDrawableBuilder.IconValue.REPEAT);
            } else if (PlayerManager.getInstance().getRepeatMode() == PlayingInfoManager.RepeatMode.SINGLE_CYCLE) {
                playModeIcon.set(MaterialDrawableBuilder.IconValue.REPEAT_ONCE);
            } else {
                playModeIcon.set(MaterialDrawableBuilder.IconValue.SHUFFLE);
            }
        }
    }

}
