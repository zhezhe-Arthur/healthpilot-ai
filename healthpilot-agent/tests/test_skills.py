
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


def test_internal_file_references_exist():
    """检查 Markdown 链接和正文中的路径，不请求外部网址。"""
    for source in SKILLS_DIR.rglob("*.md"):
        content = re.sub(r"https?://[^\s<>`）)]+", "", source.read_text(encoding="utf-8"))
        references = re.findall(r"(?<![A-Za-z0-9_./])(?:\.{1,2}/|/skills/|references/)[A-Za-z0-9_./-]+\.md", content)
        references += re.findall(r"\]\(([^)]+\.md)(?:#[^)]*)?\)", content)
        for reference in references:
            target = (SKILLS_DIR / reference.removeprefix("/skills/")) if reference.startswith("/skills/") else (source.parent / reference)
            assert target.resolve().is_relative_to(SKILLS_DIR.resolve()), f"引用超出 Skills：{source}: {reference}"
            assert target.is_file(), f"引用不存在：{source}: {reference}"


def test_smoke_script_skill_paths_exist():
    script = Path(__file__).with_name("test_agent.py").read_text(encoding="utf-8")
    for reference in re.findall(r"/skills/([A-Za-z0-9_./-]+\.md)", script):
        assert (SKILLS_DIR / reference).is_file(), reference
