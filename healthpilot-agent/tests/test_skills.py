
from pathlib import Path
import re

import pytest
import yaml


# Skills 根目录
SKILLS_DIR = Path(__file__).resolve().parents[1] / "app" / "skills"

# 当前项目的六个 Skills
SKILL_NAMES = [
    "chronic-condition-support",
    "exercise",
    "mental-health",
    "nutrition",
    "sleep",
    "weekly-planning",
]


def test_skill_files_exist():
    """检查六个 Skills 是否存在"""
    for name in SKILL_NAMES:
        skill_file = SKILLS_DIR / name / "SKILL.md"
        assert skill_file.is_file(), f"缺少 Skill 文件：{skill_file}"


@pytest.mark.parametrize("skill_name", SKILL_NAMES)
def test_skill_metadata(skill_name):
    """检查 Skill 元数据和正文"""
    skill_file = SKILLS_DIR / skill_name / "SKILL.md"
    content = skill_file.read_text(encoding="utf-8")

    # 读取 YAML frontmatter
    match = re.match(
        r"\A---\r?\n(.*?)\r?\n---[ \t]*(?:\r?\n|$)(.*)\Z",
        content,
        re.DOTALL,
    )

    assert match is not None, f"{skill_name} 缺少有效的 YAML 文件头"

    metadata = yaml.safe_load(match.group(1))
    assert isinstance(metadata, dict)

    name = metadata.get("name")
    description = metadata.get("description")

    # 名称必须符合 Agent Skills 规范
    assert isinstance(name, str)
    assert re.fullmatch(r"[a-z0-9]+(?:-[a-z0-9]+)*", name)
    assert name == skill_name

    # 必须提供非空描述
    assert isinstance(description, str)
    assert description.strip()

    # 必须包含实际技能说明
    assert match.group(2).strip(), f"{skill_name} 正文不能为空"
