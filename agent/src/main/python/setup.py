from setuptools import setup

setup(
    name = "hms-agent",
    version = "0.1.0",
    packages = ['hms_agent', 'hms_torrent'],
    # metadata for upload to PyPI
    author = "Apache Software Foundation",
    author_email = "user@hms.apache.org",
    description = "Hadoop Management System agent",
    license = "Apache License v2.0",
    keywords = "hadoop",
    url = "http://hms.apache.org",
    long_description = "This package implements the Hadoop Management System agent for install and configure software on large scale clusters.",
    platforms=["any"],
    entry_points = {
        "console_scripts": [
            "hms-agent = hms_agent.main:main",
            "hms-torrent-callback = hms_torrent.main:main",
        ],
    }
)
