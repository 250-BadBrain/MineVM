import socket
import struct
import sys
from pathlib import Path

from PIL import Image


def read_exact(sock, size):
    data = bytearray()
    while len(data) < size:
        chunk = sock.recv(size - len(data))
        if not chunk:
            raise EOFError("VNC connection closed")
        data.extend(chunk)
    return bytes(data)


def write_pixel_format(sock):
    sock.sendall(bytes([
        0, 0, 0, 0,
        32,
        24,
        0,
        1,
        0, 255,
        0, 255,
        0, 255,
        16,
        8,
        0,
        0, 0, 0,
    ]))


def write_encodings(sock):
    sock.sendall(struct.pack(">BBHI", 2, 0, 1, 0))


def request_full_frame(sock, width, height):
    sock.sendall(struct.pack(">BBHHHH", 3, 0, 0, 0, width, height))


def dump_frame(host, port, output):
    with socket.create_connection((host, port), timeout=5) as sock:
        sock.settimeout(10)
        version = read_exact(sock, 12)
        if not version.startswith(b"RFB "):
            raise RuntimeError(f"Unexpected VNC version header: {version!r}")
        sock.sendall(version)

        security_count = read_exact(sock, 1)[0]
        security_types = read_exact(sock, security_count)
        if 1 not in security_types:
            raise RuntimeError(f"VNC server does not offer None security: {security_types!r}")
        sock.sendall(b"\x01")
        security_result = struct.unpack(">I", read_exact(sock, 4))[0]
        if security_result != 0:
            raise RuntimeError(f"VNC None security rejected: {security_result}")

        sock.sendall(b"\x01")
        width, height = struct.unpack(">HH", read_exact(sock, 4))
        read_exact(sock, 16)
        name_length = struct.unpack(">I", read_exact(sock, 4))[0]
        name = read_exact(sock, name_length).decode("utf-8", errors="replace")

        write_pixel_format(sock)
        write_encodings(sock)
        request_full_frame(sock, width, height)

        while True:
            message_type = read_exact(sock, 1)[0]
            if message_type != 0:
                continue
            read_exact(sock, 1)
            rectangle_count = struct.unpack(">H", read_exact(sock, 2))[0]
            image = Image.new("RGB", (width, height))
            for _ in range(rectangle_count):
                x, y, rect_width, rect_height, encoding = struct.unpack(">HHHHI", read_exact(sock, 12))
                if encoding != 0:
                    raise RuntimeError(f"Unsupported VNC encoding: {encoding}")
                raw = read_exact(sock, rect_width * rect_height * 4)
                rect = Image.frombytes("RGBA", (rect_width, rect_height), raw, "raw", "BGRA")
                image.paste(rect.convert("RGB"), (x, y))
            image.save(output)
            print(f"saved={output}")
            print(f"framebuffer={width}x{height}")
            print(f"name={name}")
            return


if __name__ == "__main__":
    if len(sys.argv) != 4:
        print("usage: python tools/dump_vnc_frame.py HOST PORT OUTPUT.png", file=sys.stderr)
        raise SystemExit(2)
    dump_frame(sys.argv[1], int(sys.argv[2]), Path(sys.argv[3]))
