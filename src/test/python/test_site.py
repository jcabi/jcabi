# SPDX-FileCopyrightText: Copyright (c) 2012-2026 Yegor Bugayenko
# SPDX-License-Identifier: MIT

from pathlib import Path
import unittest
import xml.etree.ElementTree as ET


class SiteTest(unittest.TestCase):
    def test_analytics_uses_ga4(self):
        site = Path(__file__).resolve().parents[2] / "site" / "site.xml"
        analytics = ET.parse(site).find(
            "s:custom/s:jcabi/s:googleAnalyticsId",
            {"s": "http://maven.apache.org/SITE/2.0.0"},
        )
        self.assertIsNotNone(analytics, "The site must configure analytics")
        self.assertRegex(analytics.text or "", r"^G-[A-Z0-9]+$")
