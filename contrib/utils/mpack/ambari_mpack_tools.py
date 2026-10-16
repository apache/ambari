import os
import json
import shutil
import logging
from xml.etree.ElementTree import Element, SubElement, tostring, parse
from xml.dom import minidom
import argparse

# Configure logging
logging.basicConfig(level=logging.INFO)


class XMLUtils:
    @staticmethod
    def parse_xml(xml_path):
        tree = parse(xml_path)
        return tree.getroot()

    @staticmethod
    def create_xml_element(tag, text=None, attrib={}):
        element = Element(tag, attrib)
        if text:
            element.text = text
        return element

    @staticmethod
    def prettify_xml(element):
        return minidom.parseString(tostring(element)).toprettyxml(indent="  ")

    @staticmethod
    def write_xml_file(path, element):
        with open(path, "w") as f:
            f.write(XMLUtils.prettify_xml(element))


class FileUtils:
    @staticmethod
    def ensure_directory(path):
        os.makedirs(path, exist_ok=True)

    @staticmethod
    def remove_directory(path):
        if os.path.exists(path):
            shutil.rmtree(path)

    @staticmethod
    def copy_directory(src, dst):
        shutil.copytree(src, dst)


class Service:
    def __init__(self, name, version):
        self.name = name
        self.version = version

    @staticmethod
    def get_service_version(xml_path):
        root = XMLUtils.parse_xml(xml_path)
        version = root.find("./services/service/version").text
        version = version.replace("-", ".")
        logging.info(f"Service version: {version}")
        return version


class Artifact:
    def __init__(self, name, type, source_dir, service_version=None, service_name=None, stack_name=None,
                 stack_version=None):
        self.name = name
        self.type = type
        self.source_dir = source_dir
        self.service_version = service_version
        self.service_name = service_name
        self.stack_name = stack_name
        self.stack_version = stack_version

    def to_dict(self):
        artifact_dict = {
            "name": self.name,
            "type": self.type,
            "source_dir": self.source_dir
        }
        if self.service_version:
            artifact_dict["service_version"] = self.service_version
        if self.service_name:
            artifact_dict["service_versions_map"] = [
                {
                    "service_name": self.service_name,
                    "service_version": self.service_version,
                    "applicable_stacks": [
                        {
                            "stack_name": self.stack_name,
                            "stack_version": self.stack_version
                        }
                    ]
                }
            ]
        return artifact_dict


class Mpack:
    def __init__(self, name, version, description, min_ambari_version, max_ambari_version, artifacts=None,
                 mpack_type="service", build_number=None, branch=None):
        self.name = name
        self.version = version
        self.description = description
        self.min_ambari_version = min_ambari_version
        self.max_ambari_version = max_ambari_version
        self.artifacts = artifacts or []
        self.mpack_type = mpack_type
        self.build_number = build_number
        self.branch = branch

    def to_dict(self):
        if self.mpack_type == "service":
            return {
                "type": "full-release",
                "name": self.name,
                "version": self.version,
                "description": self.description,
                "prerequisites": {
                    "min_ambari_version": self.min_ambari_version,
                    "max-ambari-version": self.max_ambari_version
                },
                "artifacts": [artifact.to_dict() for artifact in self.artifacts]
            }
        else:
            return {
                "type": "full-release",
                "name": self.name,
                "version": self.version,
                "hash": self.build_number,
                "branch": self.branch,
                "description": self.description,
                "prerequisites": {
                    "min-ambari-version": f"{self.min_ambari_version}",
                    "max-ambari-version": f"{self.max_ambari_version}"
                },
                "artifacts": [
                    {
                        "name": "stack-definitions",
                        "type": "stack-definitions",
                        "source_dir": "stacks"
                    }
                ]
            }

    def write_mpack_json(self, target_dir):
        path = os.path.join(target_dir, "mpack.json")
        with open(path, "w") as f:
            json.dump(self.to_dict(), f, indent=2)


class MpackBuilder:
    def __init__(self, ambari_dir, output_directory, service_name, stack_name, stack_version, mpack_version):
        self.ambari_dir = ambari_dir
        self.output_directory = output_directory
        self.service_name = service_name
        self.stack_name = stack_name
        self.stack_version = stack_version
        self.mpack_version = mpack_version

    def validate_paths(self):
        stack_dir = self.get_stack_source_dir()
        if not os.path.exists(stack_dir):
            raise FileNotFoundError(f"The stack directory does not exist: {stack_dir}")
        print(f"Stack directory validation passed: {stack_dir}")

        if not os.path.exists(self.output_directory):
            raise FileNotFoundError(f"The output  directory does not exist: {self.output_directory}")
            print(f"output directory validation passed: {self.output_directory}")

        if self.service_name:
            service_dir = self.get_service_source_dir()
            if not os.path.exists(service_dir):
                raise FileNotFoundError(f"The service directory does not exist: {service_dir}")
            print(f"Service directory validation passed: {service_dir}")



    def build_mpack(self, mpack_type):
        if mpack_type == "service":
            mpack_name = f"{self.service_name.lower()}-ambari-mpack"
            source_dir = self.get_service_source_dir()
            service_version = Service.get_service_version(f"{source_dir}/metainfo.xml")
        else:  # mpack_type == "stack"
            mpack_name = "ambari-mpack"
            source_dir = self.get_stack_source_dir()
            service_version = None

        target_dir = self.prepare_target_directory(mpack_name)
        self.copy_source_to_target(source_dir, target_dir, mpack_type, service_version)
        if mpack_type == "service":
            self.create_metainfo_common(service_version, target_dir)
        self.create_mpack(target_dir, mpack_type, service_version)
        self.create_mpack_tarball(self.output_directory, mpack_name)

    def get_service_source_dir(self):
        return f"{self.ambari_dir}/ambari-server/src/main/resources/stacks/{self.stack_name}/{self.stack_version}/services/{self.service_name}"

    def get_stack_source_dir(self):
        return f"{self.ambari_dir}/ambari-server/src/main/resources"

    def prepare_target_directory(self, mpack_name):
        target_dir = os.path.join(self.output_directory, mpack_name)
        FileUtils.remove_directory(target_dir)
        FileUtils.ensure_directory(target_dir)
        os.chdir(target_dir)
        return target_dir

    def copy_source_to_target(self, source_dir, target_dir, mpack_type, service_version):
        if mpack_type == "service":
            common_dir = f"{target_dir}/common-services/{self.service_name}/{service_version}"
            FileUtils.remove_directory(common_dir)
            FileUtils.copy_directory(source_dir, common_dir)
            FileUtils.ensure_directory(f"addon-services/{self.service_name}/{service_version}")
        else:  # for stack mpack
            common_dir = f"{target_dir}/common-services/"
            stack_dir = f"{target_dir}/stacks/{self.stack_name}/"
            FileUtils.copy_directory(f"{source_dir}/common-services", common_dir)
            FileUtils.copy_directory(f"{source_dir}/stacks/{self.stack_name}", stack_dir)


    def build(self):
        if self.service_name:
            mpack_builder.build_service_mpack()
        else:
            mpack_builder.build_stack_mpack()


    def build_service_mpack(self):
        self.build_mpack("service")

    def build_stack_mpack(self):
        self.build_mpack("stack")

    def create_metainfo_common(self, service_version, target_dir):
        root = XMLUtils.create_xml_element("metainfo", attrib={"schemaVersion": "2.0"})
        services = SubElement(root, "services")
        service = SubElement(services, "service")
        SubElement(service, "name").text = self.service_name
        SubElement(service, "version").text = service_version
        SubElement(service, "extends").text = f"common-services/{self.service_name}/{service_version}"
        path = os.path.join(target_dir, f"addon-services/{self.service_name}/{service_version}/metainfo.xml")
        XMLUtils.write_xml_file(path, root)

    def create_mpack(self, target_dir, mpack_type, service_version=None):
        artifacts = []
        build_number = None
        branch = None
        if mpack_type == "service":
            artifact1 = Artifact(f"{self.service_name}-service-definitions", "service-definitions", "common-services",
                                 service_version)
            artifact2 = Artifact(f"{self.service_name.lower()}-addon-service-definitions",
                                 "stack-addon-service-definitions", "addon-services", service_version,
                                 self.service_name,
                                 self.stack_name, self.stack_version)
            artifacts = [artifact1, artifact2]
        else:
            build_number = "build_number_placeholder"
            branch = "trunk"
        mpack = Mpack(f"{self.stack_name.lower()}-ambari-mpack", self.mpack_version,
                      f"{self.stack_name} Management Pack", "2.7.5.0.0", "3.7.5.0.0", artifacts=artifacts,
                      build_number=build_number, branch=branch, mpack_type=mpack_type)
        mpack.write_mpack_json(target_dir)

    def create_mpack_tarball(self, output_directory, mpack_name):
        os.chdir(output_directory)
        tarball_name = f"{mpack_name}-{self.mpack_version}.tar.gz"
        os.system(f"tar -czvf {tarball_name} {mpack_name}")
        logging.info(f"Created mpack tarball: {tarball_name}")


def setup_options():
    parser = argparse.ArgumentParser(description='ambari Mpack Tools.')

    # Add the arguments
    parser.add_argument('-ambari-dir',
                        required=True,
                        type=str,
                        help='The Ambari project directory')
    parser.add_argument('-stack-name',
                        required=True,
                        type=str,
                        help='The Ambari stack you want to package')

    parser.add_argument('-service-name',
                        type=str,
                        help='The Ambari service you want to package')

    parser.add_argument('-stack-version',
                        type=str,
                        help='The stack version to package the service for. It is used when packaging a service mpack '
                             'to specify which version of the stack the service belongs to.')

    parser.add_argument('-mpack-version',
                        required=True,
                        type=str,
                        help='mpack verion')

    parser.add_argument('-output-dir',
                        required=True,
                        type=str,
                        help='Location to save the packaged mpack tar')

    args = parser.parse_args()
    logging.info(f"main program params is : {args}")
    return args


# Usage
if __name__ == "__main__":

    args = setup_options()
    ambari_dir = args.ambari_dir
    stack_name = args.stack_name
    stack_version = args.stack_version
    service_name = args.service_name
    mpack_version = args.mpack_version
    output_dir = args.output_dir
    ambari_dir = os.path.abspath(ambari_dir)
    output_dir = os.path.abspath(output_dir)
    mpack_builder = MpackBuilder(ambari_dir, output_dir, service_name, stack_name, stack_version, mpack_version)
    mpack_builder.validate_paths()
    mpack_builder.build()
