#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Ghi 100 câu demo (người già) vào SQLite trên máy đã cắm adb. Không nằm trong APK."""

from __future__ import annotations

import base64
import datetime
import json
import os
import sqlite3
import subprocess
import tempfile
from zoneinfo import ZoneInfo

PKG = "com.example.ViDroidCall_Studio"
REMOTE = "databases/vidroidcall_commands.db"
TZ = ZoneInfo("Asia/Ho_Chi_Minh")
NOW = int(datetime.datetime.now(TZ).timestamp() * 1000)
SPAN = 3 * 24 * 3600 * 1000 - 2 * 3600 * 1000


def j(**kw):
    return json.dumps(kw, ensure_ascii=False, separators=(",", ":"))


def meta(intent, slot, label, category, **payload):
    return dict(
        intent=intent,
        slot=f"{intent}|{slot}" if slot else None,
        label=label,
        category=category,
        json=j(type=intent, **payload) if payload else None,
    )


def call(name):
    return meta(
        "call_contact",
        name.lower(),
        f"Gọi {name}",
        "Cuộc gọi",
        requiresConfirmation=True,
        contact=name,
        phoneNumber="",
    )


def sms(name, message=""):
    return meta(
        "send_sms",
        name.lower(),
        f"Nhắn {name}",
        "Tin nhắn",
        requiresConfirmation=True,
        contact=name,
        phoneNumber="",
        message=message,
    )


def app(name, display=None):
    return meta(
        "open_app",
        name.lower(),
        f"Mở {display or name}",
        "Ứng dụng",
        requiresConfirmation=False,
        appName=name,
    )


def alarm(hour, minute=0):
    label = f"Báo thức {hour} giờ" if minute == 0 else f"Báo thức {hour} giờ {minute} phút"
    return meta(
        "set_alarm",
        f"{hour:02d}:{minute:02d}",
        label,
        "Báo thức",
        requiresConfirmation=False,
        hour=hour,
        minute=minute,
    )


def timer(n, unit, sec):
    return meta(
        "set_timer",
        f"{n}|{unit.lower()}",
        f"Hẹn giờ {n} {unit}",
        "Hẹn giờ",
        requiresConfirmation=False,
        durationSeconds=sec,
        displayDuration=n,
        unitText=unit,
    )


def video(q):
    return meta(
        "search_video",
        q.lower(),
        f"Video {q}",
        "Video",
        requiresConfirmation=False,
        query=q,
    )


def web(q):
    return meta(
        "search_web",
        q.lower(),
        f"Tìm {q}",
        "Tìm web",
        requiresConfirmation=False,
        query=q,
    )


def music(q, **extra):
    return meta(
        "play_music",
        q.lower(),
        f"Nhạc {q}",
        "Nhạc",
        requiresConfirmation=False,
        songName=extra.get("song", ""),
        artist=extra.get("artist", ""),
        genre=extra.get("genre", ""),
        musicQuery=q,
    )


def mapa(d):
    return meta(
        "open_map",
        d.lower(),
        f"Bản đồ {d}",
        "Bản đồ",
        requiresConfirmation=False,
        destination=d,
    )


def talk(intent):
    return meta(intent, None, None, "Chào hỏi" if intent == "greeting" else "Tạm biệt")


def fmt(ts: int) -> str:
    dt = datetime.datetime.fromtimestamp(ts / 1000, TZ)
    hours = (NOW - ts) / 3_600_000
    clock = dt.strftime("%H:%M")
    if hours < 24:
        return f"Hôm nay {clock}"
    if hours < 48:
        return f"Hôm qua {clock}"
    return dt.strftime("%d/%m/%Y ") + clock


def utterances():
    con, zalo, thuoc5 = call("Con"), app("zalo", "Zalo"), alarm(5)
    bolero, bacsi = video("nhạc bolero"), call("Bác sĩ")
    nhan, yt, hen30 = sms("Con", "Con ơi mẹ khỏe"), app("youtube", "YouTube"), timer(30, "phút", 1800)
    rows = [
        ("Gọi con", con),
        ("Mở Zalo", zalo),
        ("Đặt báo thức 5 giờ uống thuốc", thuoc5),
        ("Tìm video nhạc bolero", bolero),
        ("Gọi bác sĩ", bacsi),
        ("Nhắn con mẹ khỏe rồi", nhan),
        ("Mở YouTube", yt),
        ("Chỉ đường ra bệnh viện", mapa("bệnh viện")),
        ("Thời tiết hôm nay thế nào", web("thời tiết hôm nay")),
        ("Hẹn giờ 30 phút nấu cháo", hen30),
    ]
    for t in [
        "Gọi cho con", "Gọi điện cho con", "Gọi con giúp tôi", "Gọi con đi",
        "Ơi gọi con", "Gọi con gái", "Nhấc máy gọi con", "Gọi con ngay",
        "Gọi điện con", "Gọi con giúp mẹ",
    ]:
        rows.append((t, con))
    for t in [
        "Mở Zalo xem tin con", "Mở ứng dụng Zalo", "Mở Zalo giúp tôi", "Bật Zalo",
        "Vào Zalo", "Mở Zalo lên", "Mở Zalo cho tôi", "Mở Zalo nghe con nhắn", "Mở app Zalo",
    ]:
        rows.append((t, zalo))
    for t in [
        "Báo thức lúc 5 giờ", "Đặt báo thức năm giờ", "Báo thức 5 giờ sáng uống thuốc",
        "Đặt báo 5 giờ", "Báo thức năm giờ sáng", "Đặt báo thức 5 giờ giúp tôi",
        "Nhắc tôi 5 giờ uống thuốc",
    ]:
        rows.append((t, thuoc5))
    for t in [
        "Video nhạc bolero", "Mở video bolero", "Tìm video bolero",
        "Phát video nhạc bolero", "Tìm trên YouTube nhạc bolero", "Nghe video bolero",
    ]:
        rows.append((t, bolero))
    for t in ["Gọi điện bác sĩ", "Gọi cho bác sĩ", "Gọi bác sĩ giúp tôi", "Gọi bác sĩ đi"]:
        rows.append((t, bacsi))
    for t in ["Nhắn con", "Nhắn tin cho con", "Gửi tin nhắn cho con", "Nhắn con tôi đã uống thuốc"]:
        rows.append((t, nhan))
    for t in ["Mở Youtube", "Bật YouTube", "Vào YouTube", "Mở YouTube nghe cải lương"]:
        rows.append((t, yt))
    rows += [("Hẹn giờ nửa tiếng", hen30), ("Đặt hẹn giờ 30 phút", hen30)]
    rows += [
        ("Gọi con trai", call("Con trai")),
        ("Gọi cháu Minh", call("Minh")),
        ("Gọi cháu gái", call("Cháu")),
        ("Gọi hàng xóm", call("Hàng xóm")),
        ("Gọi nhà thuốc", call("Nhà thuốc")),
        ("Nhắn con trai mẹ ra chợ", sms("Con trai", "Mẹ ra chợ")),
        ("Nhắn cháu nhớ về ăn cơm", sms("Minh", "Nhớ về ăn cơm")),
        ("Mở Camera", app("camera", "Máy ảnh")),
        ("Mở Cài đặt", app("settings", "Cài đặt")),
        ("Mở Facebook", app("facebook", "Facebook")),
        ("Mở Messenger", app("messenger", "messenger")),
        ("Chỉ đường ra chợ", mapa("chợ")),
        ("Tìm đường tới nhà thuốc", mapa("nhà thuốc")),
        ("Chỉ đường về nhà", mapa("nhà")),
        ("Đặt báo thức 6 giờ tập dưỡng sinh", alarm(6)),
        ("Đặt báo thức 7 giờ 30 ăn sáng", alarm(7, 30)),
        ("Đặt báo thức 20 giờ uống thuốc tối", alarm(20)),
        ("Hẹn giờ 5 phút", timer(5, "phút", 300)),
        ("Hẹn giờ 10 phút canh thuốc", timer(10, "phút", 600)),
        ("Tìm video dưỡng sinh", video("dưỡng sinh")),
        ("Tìm video cải lương", video("cải lương")),
        ("Tìm video tin tức", video("tin tức")),
        ("Tìm giá vàng hôm nay", web("giá vàng hôm nay")),
        ("Tìm lịch âm hôm nay", web("lịch âm hôm nay")),
        ("Tìm nhà thuốc gần đây", web("nhà thuốc gần đây")),
        ("Phát nhạc vàng", music("nhạc vàng", genre="nhạc vàng")),
        ("Phát nhạc Trịnh Công Sơn", music("Trịnh Công Sơn", artist="Trịnh Công Sơn")),
        ("Phát bài Cát bụi", music("Cát bụi", song="Cát bụi")),
        ("Phát nhạc bolero", music("bolero", genre="bolero")),
        ("Xin chào", talk("greeting")),
        ("Chào buổi sáng", talk("greeting")),
        ("Tạm biệt", talk("goodbye")),
        ("Thôi nghỉ nghe con", talk("goodbye")),
        ("Gọi con dâu", call("Con dâu")),
        ("Gọi em út", call("Em út")),
        ("Nhắn con nhớ đóng tiền điện", sms("Con", "Nhớ đóng tiền điện")),
        ("Mở Maps", app("maps", "maps")),
        ("Chỉ đường tới ủy ban", mapa("ủy ban")),
        ("Đặt báo thức 12 giờ nghỉ trưa", alarm(12)),
        ("Hẹn giờ 1 giờ", timer(1, "giờ", 3600)),
        ("Tìm video hát chèo", video("hát chèo")),
        ("Tìm giờ khám bệnh viện", web("giờ khám bệnh viện")),
        ("Phát nhạc quê hương", music("nhạc quê hương", genre="nhạc quê hương")),
        ("Trợ lý ơi chào bà", talk("greeting")),
    ]
    if len(rows) != 100:
        raise SystemExit(f"cần 100 câu, đang {len(rows)}")
    return rows


def main():
    raw = subprocess.check_output(["adb", "exec-out", "run-as", PKG, "cat", REMOTE])
    if not raw.startswith(b"SQLite format 3"):
        raise SystemExit("Mở app một lần trên điện thoại rồi chạy lại.")

    fd, path = tempfile.mkstemp(suffix=".db")
    os.close(fd)
    open(path, "wb").write(raw)
    rows = utterances()
    conn = sqlite3.connect(path)
    cur = conn.cursor()
    cur.execute("DELETE FROM command_events")
    cur.execute("DELETE FROM command_meta")
    cur.execute("DELETE FROM sqlite_sequence WHERE name='command_events'")
    for i, (text, m) in enumerate(rows):
        ts = NOW - i * SPAN // (len(rows) - 1)
        cur.execute(
            """INSERT INTO command_events
               (command_text,category,status,time_formatted,timestamp,slot_key,intent,label,action_json)
               VALUES (?,?,?,?,?,?,?,?,?)""",
            (text, m["category"], "Thành công", fmt(ts), ts, m["slot"], m["intent"], m["label"], m["json"]),
        )
    day = datetime.datetime.fromtimestamp(NOW / 1000, TZ).strftime("%Y-%m-%d")
    snap = json.dumps(
        [
            "call_contact|con",
            "open_app|zalo",
            "set_alarm|05:00",
            "search_video|nhạc bolero",
            "call_contact|bác sĩ",
        ],
        ensure_ascii=False,
        separators=(",", ":"),
    )
    cur.executemany(
        "INSERT INTO command_meta(meta_key,meta_value) VALUES (?,?)",
        [("last_purge_day", day), ("snapshot_day", day), ("snapshot_keys", snap)],
    )
    conn.commit()
    conn.close()

    subprocess.check_call(["adb", "shell", "am", "force-stop", PKG])
    payload = base64.b64encode(open(path, "rb").read())
    subprocess.run(
        ["adb", "shell", f"run-as {PKG} sh -c 'base64 -d > {REMOTE} && chmod 660 {REMOTE}'"],
        input=payload + b"\n",
        check=True,
    )
    os.remove(path)
    print("Xong. Mở lại app → tab Lịch sử.")


if __name__ == "__main__":
    main()
