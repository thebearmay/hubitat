/*
 * Icon Display
 * 
 *
 *  Licensed Virtual the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WIyTHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *    Date            Who                    Description
 *    -------------   -------------------    ---------------------------------------------------------
 *    20Jul2024        thebearmay            v0.0.1 - Original code
 */
    


static String version()	{  return '0.0.1'  }
import groovy.transform.Field

definition (
	name: 			"Icon Display", 
	namespace: 		"thebearmay", 
	author: 		"Jean P. May, Jr.",
	description: 	"App to show all icons in materials-icons",
	category: 		"Utility",
	importUrl: "https://raw.githubusercontent.com/thebearmay/hubitat/main/apps/iconDisplay.groovy",
    installOnOpen:  true,
	oauth: 			true,
    iconUrl:        "",
    iconX2Url:      ""
) 

preferences {
    page name: "mainPage"
}

mappings {
}

def installed() {
//	log.trace "installed()"
    state?.isInstalled = true
    initialize()
}

def updated(){
//	log.trace "updated()"
    if(!state?.isInstalled) { state?.isInstalled = true }
	if(debugEnable) runIn(1800,logsOff)
}

def initialize(){
}

void logsOff(){
     app.updateSetting("debugEnabled",[value:"false",type:"bool"])
}

def mainPage(){
    dynamicPage (name: "mainPage", title: "", install: true, uninstall: true) {
         section(name:"Icon Display",title:"Materials Icons", hideable: true, hidden: false){
             oPut = divHeader+"<script>function cpy2Clip(elem){try {navigator.clipboard.textWrite(elem.id);}catch (err){alert(elem.id);}buttonClick(elem)}</script>"
             iconList.each{
                 if("$it".contains('-')){
                    oPut+="<i id='$it' class='material-icons $it' style='font-size:36px; padding:4px 4px 0px; height:54px; line-height:36px;' onclick='cpy2Clip(this)'></i>"
                 }else if(it != 'block'){
                 	oPut+="<i id='$it' class='material-icons $it' style='font-size:36px; padding:4px 4px 0px; height:54px; line-height:36px;' onclick='cpy2Clip(this)'>$it</i>"
                 }else{
                    oPut+="<i id='$it' class='material-icons' style='font-size:36px; padding:4px 4px 0px; height:54px; line-height:36px;' onclick='cpy2Clip(this)'>$it</i>" 
                 }
             }
             oPut+='</div></div>'
             paragraph "$oPut"
         }
         section("Change Application Name", hideable: true, hidden: true){
            input "nameOverride", "text", title: "New Name for Application", multiple: false, required: false, submitOnChange: true, defaultValue: app.getLabel()
            if(nameOverride != app.getLabel()) app.updateLabel(nameOverride)
         }
    }
}


def appButtonHandler(btn) {
	switch(btn) {
		default: 
			log.error "Undefined button $btn pushed"
        	break
    }
}

@Field static String divHeader = '<div class="h-400 overflow-auto"><div id="fonts" style="display: inline-block; cursor: pointer;">'
@Field static ArrayList iconList = ["3d_rotation","ac_unit","access_alarm","access_alarms","access_time","accessibility","accessible","account_balance","account_balance_wallet","account_box","account_circle","adb","add","add_a_photo","add_alarm","add_alert","add_box","add_circle","add_circle_outline","add_location","add_shopping_cart","add_to_photos","add_to_queue","adjust","airline_seat_flat","airline_seat_flat_angled","airline_seat_individual_suite","airline_seat_legroom_extra","airline_seat_legroom_normal","airline_seat_legroom_reduced","airline_seat_recline_extra","airline_seat_recline_normal","airplanemode_active","airplanemode_inactive","airplay","airport_shuttle","alarm","alarm_add","alarm_off","alarm_on","album","all_inclusive","all_out","android","announcement","apps","archive","arrow_back","arrow_downward","arrow_drop_down","arrow_drop_down_circle","arrow_drop_up","arrow_forward","arrow_upward","art_track","aspect_ratio","assessment","assignment","assignment_ind","assignment_late","assignment_return","assignment_returned","assignment_turned_in","assistant","assistant_photo","attach_file","attach_money","attachment","audiotrack","autorenew","av_timer","backspace","backup","battery_alert","battery_charging_full","battery_full","battery_std","battery_unknown","beach_access","beenhere","block","bluetooth","bluetooth_audio","bluetooth_connected","bluetooth_disabled","bluetooth_searching","blur_circular","blur_linear","blur_off","blur_on","book","bookmark","bookmark_border","border_all","border_bottom","border_clear","border_color","border_horizontal","border_inner","border_left","border_outer","border_right","border_style","border_top","border_vertical","branding_watermark","brightness_1","brightness_2","brightness_3","brightness_4","brightness_5","brightness_6","brightness_7","brightness_auto","brightness_high","brightness_low","brightness_medium","broken_image","brush","bubble_chart","bug_report","build","burst_mode","business","business_center","cached","cake","call","call_end","call_made","call_merge","call_missed","call_missed_outgoing","call_received","call_split","call_to_action","camera","camera_alt","camera_enhance","camera_front","camera_rear","camera_roll","cancel","card_giftcard","card_membership","card_travel","casino","cast","cast_connected","center_focus_strong","center_focus_weak","change_history","chat","chat_bubble","chat_bubble_outline","check","check_box","check_box_outline_blank","check_circle","chevron_left","chevron_right","child_care","child_friendly","chrome_reader_mode","class","clear","clear_all","close","closed_caption","cloud","cloud_circle","cloud_done","cloud_download","cloud_off","cloud_queue","cloud_upload","code","collections","collections_bookmark","color_lens","colorize","comment","compare","compare_arrows","computer","confirmation_number","contact_mail","contact_phone","contacts","content_copy","content_cut","content_paste","control_point","control_point_duplicate","copyright","create","create_new_folder","credit_card","crop","crop_16_9","crop_3_2","crop_5_4","crop_7_5","crop_din","crop_free","crop_landscape","crop_original","crop_portrait","crop_rotate","crop_square","dashboard","data_usage","date_range","dehaze","delete","delete_forever","delete_sweep","description","desktop_mac","desktop_windows","details","developer_board","developer_mode","device_hub","devices","devices_other","dialer_sip","dialpad","directions","directions_bike","directions_boat","directions_bus","directions_car","directions_railway","directions_run","directions_subway","directions_transit","directions_walk","disc_full","dns","do_not_disturb","do_not_disturb_alt","do_not_disturb_off","do_not_disturb_on","dock","domain","done","done_all","donut_large","donut_small","drafts","drag_handle","drive_eta","dvr","edit","edit_location","eject","email","enhanced_encryption","equalizer","error","error_outline","euro_symbol","ev_station","event","event_available","event_busy","event_note","event_seat","exit_to_app","expand_less","expand_more","explicit","explore","exposure","exposure_neg_1","exposure_neg_2","exposure_plus_1","exposure_plus_2","exposure_zero","extension","face","fast_forward","fast_rewind","favorite","favorite_border","featured_play_list","featured_video","feedback","fiber_dvr","fiber_manual_record","fiber_new","fiber_pin","fiber_smart_record","file_download","file_upload","filter","filter_1","filter_2","filter_3","filter_4","filter_5","filter_6","filter_7","filter_8","filter_9","filter_9_plus","filter_b_and_w","filter_center_focus","filter_drama","filter_frames","filter_hdr","filter_list","filter_none","filter_tilt_shift","filter_vintage","find_in_page","find_replace","fingerprint","first_page","fitness_center","flag","flare","flash_auto","flash_off","flash_on","flight","flight_land","flight_takeoff","flip","flip_to_back","flip_to_front","folder","folder_open","folder_shared","folder_special","font_download","format_align_center","format_align_justify","format_align_left","format_align_right","format_bold","format_clear","format_color_fill","format_color_reset","format_color_text","format_indent_decrease","format_indent_increase","format_italic","format_line_spacing","format_list_bulleted","format_list_numbered","format_paint","format_quote","format_shapes","format_size","format_strikethrough","format_textdirection_l_to_r","format_textdirection_r_to_l","format_underlined","forum","forward","forward_10","forward_30","forward_5","free_breakfast","fullscreen","fullscreen_exit","functions","g_translate","gamepad","games","gavel","gesture","get_app","gif","goat","golf_course","gps_fixed","gps_not_fixed","gps_off","grade","gradient","grain","graphic_eq","grid_off","grid_on","group","group_add","group_work","hd","hdr_off","hdr_on","hdr_strong","hdr_weak","he-acceleration_active","he-acceleration_inactive","he-add_1","he-add_2","he-address-book1","he-advanced_1","he-aid-kit","he-air_filter","he-alarm","he-alarm1","he-alarm_2","he-alexa","he-amazon2","he-android1","he-apple1","he-appointment","he-apps_1","he-apps_11","he-apps_2","he-apps_21","he-arrival","he-arrow-down-left2","he-arrow-down-right2","he-arrow-down2","he-arrow-left2","he-arrow-right2","he-arrow-up-left2","he-arrow-up-right2","he-arrow-up2","he-attachment","he-axis","he-axis_2","he-axis_3","he-backward2","he-barcode1","he-bathtub1","he-battery_25","he-battery_25_color","he-battery_25_filled","he-battery_50","he-battery_50_color","he-battery_50_filled","he-battery_75_color","he-battery_75_to_100","he-battery_empty","he-battery_empty_filled","he-battery_full","he-battery_full_color","he-battery_large","he-battery_large_filled","he-battery_low_color","he-beach-chair","he-bed_1","he-bed_2","he-bed_3","he-bed_4","he-bed_5","he-bell1","he-bin","he-bin2","he-blind1","he-blocked","he-bluetooth1","he-bluetooth_headphones","he-book1","he-bookmark1","he-bookmarks","he-books","he-bottom_line","he-box-add","he-box-remove","he-briefcase1","he-brightness-contrast","he-bubble","he-bubble2","he-bubbles","he-bubbles2","he-bubbles3","he-bubbles4","he-bug1","he-bulb_1","he-bulb_2","he-bulb_4","he-bulb_6","he-bulb_off","he-bulb_on","he-calculator1","he-calendar1","he-calendar2","he-calendar_2","he-camera1","he-cancel-circle","he-car1","he-cctv","he-cd_rom","he-ceiling_lamp","he-ceiling_lamp_2","he-chair_lounge","he-chandelier_2","he-checkbox-checked","he-checkbox-unchecked","he-checkmark","he-checkmark2","he-chrome1","he-circle-down","he-circle-left","he-circle-right","he-circle-up","he-circuit","he-clean","he-cleaning_1","he-cleaning_2","he-cleaning_3","he-clipboard1","he-clock","he-clock2","he-cloud-check","he-cloud-download1","he-cloud-upload1","he-cloud1","he-co2","he-codepen1","he-cog1","he-cogs1","he-color_palette","he-command","he-community_1","he-community_2","he-compass1","he-compass2","he-computer","he-computer_2","he-connection","he-contact_closed","he-contact_open","he-contrast","he-copy1","he-crop1","he-cross","he-ctrl","he-customer_support","he-danger","he-dashboard1","he-dashboards","he-database1","he-default_dashboard_icon","he-delicious1","he-devices_1","he-devices_2","he-dimmer_high","he-dimmer_low","he-dimmer_medium","he-dining-chair","he-dinner","he-discover","he-discovery_1","he-discovery_3","he-display","he-door-1","he-door_3","he-door_closed","he-door_enter","he-door_exit_2","he-door_lock","he-door_open","he-door_remote","he-download1","he-download2","he-download3","he-downstairs","he-drawer","he-drawer2","he-dribbble1","he-drive","he-drive1","he-drop","he-dropbox1","he-dryer","he-dryer_2","he-earth","he-edge1","he-eject1","he-electricity","he-embed","he-embed2","he-enlarge","he-enlarge2","he-enter","he-envelop","he-equalizer","he-equalizer2","he-ethernet","he-events_2","he-exclude_1","he-exclude_3","he-exit","he-eye-blocked","he-eye-minus","he-eye-plus","he-eye1","he-eyedropper1","he-facebook1","he-facebook2","he-facebook3","he-facebook_2","he-fan","he-fan_2","he-fan_auto","he-fan_high","he-fan_low","he-fan_med","he-fan_med_high","he-fan_med_low","he-fan_off","he-fan_on","he-feed1","he-file-empty","he-file-music","he-file-picture","he-file-play","he-file-text1","he-file-text2","he-file-video","he-file-zip","he-file1","he-file_2","he-files-empty","he-film1","he-filter1","he-filter2","he-finder","he-fingerprint","he-fire-alarm","he-fire1","he-fire2","he-fire_alarm_clear","he-firefox1","he-fireplace","he-first","he-fist","he-fist_2","he-flag1","he-flag2","he-flash1","he-flickr1","he-flickr2","he-flickr3","he-flickr4","he-floppy-disk","he-folder-download","he-folder-minus","he-folder-open1","he-folder-plus","he-folder-upload","he-folder1","he-forward1","he-forward3","he-gaming_controler","he-gaming_controller_2","he-gaming_controller_3","he-garage_closed","he-garage_open","he-gas_warning","he-gauge","he-gauge_2","he-gift1","he-git1","he-github1","he-glass1","he-glass2","he-gmail","he-google-drive","he-google1","he-google2","he-google3","he-google_play","he-grid","he-hammer","he-hand_3","he-hand_wave","he-hanging_lights","he-hanging_roof_lamp","he-hangouts","he-headphones1","he-heart1","he-help1","he-help_1","he-help_2","he-history1","he-home1","he-home2","he-home3","he-hour-glass","he-house_1","he-house_2","he-house_3","he-house_4","he-house_5","he-hubevents_2","he-humidity","he-humidity_filled","he-image1","he-images","he-infinite","he-info1","he-info_1","he-info_3","he-insert-template","he-instagram1","he-integrated_circuit","he-key1","he-keyboard","he-keypad_1","he-keypad_2","he-kitchen","he-lab","he-lamp_1","he-lamp_hanging","he-laptop1","he-last","he-laundry","he-leaf1","he-less_plus_signs","he-lifebuoy","he-lightbulb","he-link1","he-link2","he-link_broken","he-linkedin1","he-linkedin2","he-list-numbered","he-list1","he-list2","he-list3","he-location","he-location2","he-location_1","he-location_3","he-locationevents_2","he-lock1","he-logo-mark","he-logo-vertical-white","he-logs_2","he-logs_4","he-long_bottom_line","he-loop","he-loop2","he-magic-wand","he-magnet1","he-man","he-man-woman","he-map1","he-map2","he-menu","he-menu2","he-menu3","he-menu4","he-meter","he-meter1","he-meter2","he-mic","he-microsoft","he-microwave","he-minus1","he-mobile1","he-mobile2","he-mode_away","he-mode_cleaning","he-mode_day","he-mode_default","he-mode_evening","he-mode_night","he-mode_vacation","he-monitor","he-motion-sensor","he-motion_detector_1","he-motion_detector_2","he-motion_detector_3","he-motion_detector_3_filled","he-motion_detector_4","he-mug","he-music1","he-music_player","he-mute","he-netflix","he-new-tab","he-newspaper","he-next2","he-next_track","he-night_1","he-night_2","he-not_present","he-notification","he-oculus_rift","he-office","he-onedrive","he-opt","he-outlet","he-outlet_3","he-outlet_off","he-padlock_locked","he-padlock_unlocked","he-paint-format","he-pantone_1","he-paste1","he-pause-outlined-big-symbol","he-pause2","he-pause_circle","he-pencil1","he-phone-hang-up","he-phone1","he-pie-chart1","he-pinterest3","he-placeholder","he-placeholder_1","he-play1","he-play3","he-play_button","he-playstation","he-playstation_logo","he-plug1","he-plug_1","he-plug_2","he-plug_3","he-plug_4","he-plug_5","he-plug_6","he-plus1","he-podcast1","he-portrait","he-power","he-power-cord","he-power_off","he-power_on","he-present","he-previous2","he-price-tag","he-price-tags","he-printer","he-profile","he-property","he-psp","he-pushpin","he-qrcode1","he-question1","he-question2","he-question_1","he-question_2","he-quotes-left","he-quotes-right","he-radio-checked","he-radio-checked2","he-radio-unchecked","he-reboothub_1","he-reboothub_2","he-reddit1","he-redo","he-redo2","he-relay_off","he-relay_on","he-repair1","he-repair_1","he-repair_2","he-reply1","he-resize","he-road1","he-rocket1","he-router_1","he-router_2","he-router_3","he-router_4","he-rss1","he-rss2","he-running","he-safari1","he-samsung","he-scissors1","he-search1","he-security_system","he-sensor","he-sensor_1","he-sensor_2","he-sensor_3","he-sensor_4","he-sensor_5","he-settings1","he-settings_1","he-setup","he-shades_closed","he-shades_open","he-shades_partially_open","he-share1","he-share2","he-shield1","he-shift","he-shower1","he-shrink","he-shrink2","he-shuffle","he-shutdown_1","he-shutdown_3","he-sink","he-sink_2","he-siren","he-siren_and_alarm","he-skype1","he-smoke_detector","he-smoke_detector_2","he-snowflake","he-sofa","he-sound_on","he-sound_on_loud","he-soundcloud1","he-soundcloud2","he-speaker","he-speaker_1","he-speaker_2","he-speaker_3","he-speaker_off","he-sphere","he-spinner11","he-spoon-knife","he-spotify1","he-spotify2","he-square-measument","he-stack","he-stackoverflow","he-stairs","he-star-empty","he-star-full","he-star-half1","he-stats-bars","he-stats-bars2","he-stats-dots","he-steam1","he-steam2","he-stop2","he-stop3","he-stop_2","he-stop_2_filled","he-stopwatch","he-stove","he-strikethrough2","he-sun","he-sun1","he-sunrise","he-sunrise_2","he-sunrise_3","he-suspension","he-svg","he-switch","he-switch_2","he-switch_2_flipped","he-switch_2_off","he-switch_2_off_filled","he-switch_2_on","he-switch_2_on_filled","he-switch_8","he-switch_8_closed","he-switch_off","he-switch_on","he-tab","he-table1","he-table2","he-table3","he-tablet1","he-tap","he-target","he-telegram1","he-television_1","he-television_2","he-terminal1","he-thermometer1","he-thermometer_2","he-thermometer_3","he-thermometer_4","he-thermometer_5","he-thermostat_6","he-thumb-up","he-ticket1","he-toilet","he-tree1","he-trello1","he-trophy1","he-tumblr1","he-tumblr2","he-tux","he-tv1","he-twitch1","he-twitter1","he-twitter2","he-undo1","he-undo2","he-ungroup","he-unlink1","he-unlocked","he-upload1","he-upload2","he-upload3","he-user-check","he-user-minus","he-user-plus1","he-user-tie","he-user1","he-users1","he-valve","he-valve_2","he-valve_3","he-valve_4","he-ventilator","he-video-camera1","he-video-player","he-vimeo1","he-vimeo2","he-volume-decrease","he-volume-high","he-volume-increase","he-volume-low","he-volume-medium","he-volume-mute","he-volume-mute2","he-wardrobe","he-wardrobe_2","he-warning1","he-washing_machine","he-washing_machine_2","he-washing_machine_3","he-washing_machine_4","he-water","he-water_2","he-water_dry","he-water_wet","he-web-design","he-whatsapp1","he-wifi1","he-wikipedia","he-window","he-window_1","he-window_2","he-window_3","he-windows1","he-windows8","he-woman","he-wordpress1","he-wrench1","he-xbox","he-youtube1","he-youtube3","he-zigbee","he-zoom-in","he-zoom-out","he-zwave","headset","headset_mic","healing","hearing","help","help_outline","high_quality","highlight","highlight_off","history","home","hot_tub","hotel","hourglass_empty","hourglass_full","http","https","image","image_aspect_ratio","import_contacts","import_export","important_devices","inbox","indeterminate_check_box","info","info_outline","input","insert_chart","insert_comment","insert_drive_file","insert_emoticon","insert_invitation","insert_link","insert_photo","invert_colors","invert_colors_off","iso","keyboard","keyboard_arrow_down","keyboard_arrow_left","keyboard_arrow_right","keyboard_arrow_up","keyboard_backspace","keyboard_capslock","keyboard_hide","keyboard_return","keyboard_tab","keyboard_voice","kitchen","label","label_outline","landscape","language","laptop","laptop_chromebook","laptop_mac","laptop_windows","last_page","launch","layers","layers_clear","leak_add","leak_remove","lens","library_add","library_books","library_music","lightbulb_outline","line_style","line_weight","linear_scale","link","linked_camera","list","live_help","live_tv","local_activity","local_airport","local_atm","local_bar","local_cafe","local_car_wash","local_convenience_store","local_dining","local_drink","local_florist","local_gas_station","local_grocery_store","local_hospital","local_hotel","local_laundry_service","local_library","local_mall","local_movies","local_offer","local_parking","local_pharmacy","local_phone","local_pizza","local_play","local_post_office","local_printshop","local_see","local_shipping","local_taxi","location_city","location_disabled","location_off","location_on","location_searching","lock","lock_open","lock_outline","looks","looks_3","looks_4","looks_5","looks_6","looks_one","looks_two","loop","loupe","low_priority","loyalty","mail","mail_outline","map","markunread","markunread_mailbox","memory","menu","merge_type","message","mic","mic_none","mic_off","mms","mode_comment","mode_edit","monetization_on","money_off","monochrome_photos","mood","mood_bad","more","more_horiz","more_vert","motorcycle","mouse","move_to_inbox","movie","movie_creation","movie_filter","multiline_chart","music_note","music_video","my_location","nature","nature_people","navigate_before","navigate_next","navigation","near_me","network_cell","network_check","network_locked","network_wifi","new_releases","next_week","nfc","no_encryption","no_sim","not_interested","note","note_add","notifications","notifications_active","notifications_none","notifications_off","notifications_paused","offline_pin","ondemand_video","opacity","open_in_browser","open_in_new","open_with","pages","pageview","palette","pan_tool","panorama","panorama_fish_eye","panorama_horizontal","panorama_vertical","panorama_wide_angle","party_mode","pause","pause_circle_filled","pause_circle_outline","payment","people","people_outline","perm_camera_mic","perm_contact_calendar","perm_data_setting","perm_device_information","perm_identity","perm_media","perm_phone_msg","perm_scan_wifi","person","person_add","person_outline","person_pin","person_pin_circle","personal_video","pets","phone","phone_android","phone_bluetooth_speaker","phone_forwarded","phone_in_talk","phone_iphone","phone_locked","phone_missed","phone_paused","phonelink","phonelink_erase","phonelink_lock","phonelink_off","phonelink_ring","phonelink_setup","photo","photo_album","photo_camera","photo_filter","photo_library","photo_size_select_actual","photo_size_select_large","photo_size_select_small","picture_as_pdf","picture_in_picture","picture_in_picture_alt","pie_chart","pie_chart_outlined","pin_drop","place","play_arrow","play_circle_filled","play_circle_outline","play_for_work","playlist_add","playlist_add_check","playlist_play","plus_one","poll","polymer","pool","portable_wifi_off","portrait","power","power_input","power_settings_new","pregnant_woman","present_to_all","print","priority_high","public","publish","query_builder","question_answer","queue","queue_music","queue_play_next","radio","radio_button_checked","radio_button_unchecked","rate_review","receipt","recent_actors","record_voice_over","redeem","redo","refresh","remove","remove_circle","remove_circle_outline","remove_from_queue","remove_red_eye","remove_shopping_cart","reorder","repeat","repeat_one","replay","replay_10","replay_30","replay_5","reply","reply_all","report","report_problem","restaurant","restaurant_menu","restore","restore_page","ring_volume","room","room_service","rotate_90_degrees_ccw","rotate_left","rotate_right","rounded_corner","router","rowing","rss_feed","rv_hookup","satellite","save","scanner","schedule","school","screen_lock_landscape","screen_lock_portrait","screen_lock_rotation","screen_rotation","screen_share","sd_card","sd_storage","search","security","select_all","send","sentiment_dissatisfied","sentiment_neutral","sentiment_satisfied","sentiment_very_dissatisfied","sentiment_very_satisfied","settings","settings_applications","settings_backup_restore","settings_bluetooth","settings_brightness","settings_cell","settings_ethernet","settings_input_antenna","settings_input_component","settings_input_composite","settings_input_hdmi","settings_input_svideo","settings_overscan","settings_phone","settings_power","settings_remote","settings_system_daydream","settings_voice","share","shop","shop_two","shopping_basket","shopping_cart","short_text","show_chart","shuffle","signal_cellular_4_bar","signal_cellular_connected_no_internet_4_bar","signal_cellular_no_sim","signal_cellular_null","signal_cellular_off","signal_wifi_4_bar","signal_wifi_4_bar_lock","signal_wifi_off","sim_card","sim_card_alert","skip_next","skip_previous","slideshow","slow_motion_video","smartphone","smoke_free","smoking_rooms","sms","sms_failed","snooze","sort","sort_by_alpha","spa","space_bar","speaker","speaker_group","speaker_notes","speaker_notes_off","speaker_phone","spellcheck","star","star_border","star_half","stars","stay_current_landscape","stay_current_portrait","stay_primary_landscape","stay_primary_portrait","stop","stop_screen_share","storage","store","store_mall_directory","straighten","streetview","strikethrough_s","style","subdirectory_arrow_left","subdirectory_arrow_right","subject","subscriptions","subtitles","subway","supervisor_account","surround_sound","swap_calls","swap_horiz","swap_vert","swap_vertical_circle","switch_camera","switch_video","sync","sync_disabled","sync_problem","system_update","system_update_alt","tab","tab_unselected","tablet","tablet_android","tablet_mac","tag_faces","tap_and_play","terrain","text_fields","text_format","textsms","texture","theaters","thumb_down","thumb_up","thumbs_up_down","time_to_leave","timelapse","timeline","timer","timer_10","timer_3","timer_off","title","toc","today","toll","tonality","touch_app","toys","track_changes","traffic","train","tram","transfer_within_a_station","transform","translate","trending_down","trending_flat","trending_up","tune","turned_in","turned_in_not","tv","unarchive","undo","unfold_less","unfold_more","update","usb","verified_user","vertical_align_bottom","vertical_align_center","vertical_align_top","vibration","video_call","video_label","video_library","videocam","videocam_off","videogame_asset","view_agenda","view_array","view_carousel","view_column","view_comfy","view_compact","view_day","view_headline","view_list","view_module","view_quilt","view_stream","view_week","vignette","visibility","visibility_off","voice_chat","voicemail","volume_down","volume_mute","volume_off","volume_up","vpn_key","vpn_lock","wallpaper","warning","watch","watch_later","wb_auto","wb_cloudy","wb_incandescent","wb_iridescent","wb_sunny","wc","web","web_asset","weekend","whatshot","widgets","wifi","wifi_lock","wifi_tethering","work","wrap_text","youtube_searched_for","zoom_in","zoom_out","zoom_out_map"]
