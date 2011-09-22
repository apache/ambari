from setuptools import setup

setup(
    name = "ambari-agent",
    version = "0.1.0",
    packages = ['ambari_agent', 'ambari_torrent'],
    # metadata for upload to PyPI
    author = "Apache Software Foundation",
    author_email = "ambari-dev@incubator.apache.org",
    description = "Ambari agent",
    license = "Apache License v2.0",
    keywords = "hadoop, ambari",
    url = "http://incubator.apache.org/ambari",
    long_description = "This package implements the Ambari agent for installing Hadoop on large clusters.",
    platforms=["any"],
    entry_points = {
        "console_scripts": [
            "ambari-agent = ambari_agent.main:main",
            "ambari-torrent-callback = hms_torrent.main:main",
        ],
    }
)
