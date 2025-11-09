import { ChangeEvent, useState } from 'react'
import { Button, Form, FormControl, Modal } from 'react-bootstrap'
import toast from 'react-hot-toast'
import { get } from 'lodash'
import DefaultButton from '../DefaultButton'
import VersionsApi from '../../api/versions'
import { ReadOptions } from '../../constants'

type ModalProps = {
  isOpen: boolean
  onClose: () => void
  onReadVersion: (versionResources: unknown) => void
}

const AddVersionModal = ({ isOpen, onClose, onReadVersion }: ModalProps) => {
  const [uploadOption, setUploadOption] = useState<ReadOptions>(ReadOptions.FILE)
  const [file, setFile] = useState<File | undefined>()
  const [fileUrl, setFileUrl] = useState<string>('')

  const readVersionInfo = async () => {
    try {
      if (uploadOption === ReadOptions.FILE && file) {
        const reader = new FileReader()
        reader.onload = async (event: ProgressEvent<FileReader>) => {
          const fileContents = get(event, 'target.result', undefined)
          const versionResources = await VersionsApi.readVersionInfo(
            fileContents,
            {
              'Content-Type': 'text/xml',
            },
          )
          onReadVersion(versionResources)
        }
        reader.readAsText(file)
      } else if (uploadOption === ReadOptions.URL && fileUrl) {
        const versionResources = await VersionsApi.readVersionInfo({
          VersionDefinition: {
            version_url: fileUrl,
          },
        })
        onReadVersion(versionResources)
      }
    } catch (err) {
      toast.error('Could not read version definition')
    }
  }

  const handleClose = () => {
    setFile(undefined)
    setFileUrl('')
    onClose()
  }

  return (
    <Modal show={isOpen} onHide={onClose} data-testid="add-version-modal">
      <Modal.Header>
        <Modal.Title>Add Version</Modal.Title>
      </Modal.Header>
      <Modal.Body>
        <Form.Check
          checked={uploadOption === ReadOptions.FILE}
          type="radio"
          id={ReadOptions.FILE}
          onChange={() => {
            setUploadOption(ReadOptions.FILE)
          }}
          label="Upload Version Definition File"
          className="px-0"
        />
        <FormControl
          type="file"
          className="py-1"
          onChange={(event: ChangeEvent<HTMLInputElement>) => {
            setFile(event?.target?.files?.[0])
          }}
          disabled={uploadOption !== ReadOptions.FILE}
        />
        <Form.Check
          id={ReadOptions.URL}
          checked={uploadOption === ReadOptions.URL}
          onChange={() => {
            setUploadOption(ReadOptions.URL)
          }}
          type="radio"
          label="Version Definition File URL"
          className="px-0 mt-3"
        />
        <FormControl
          type="text"
          className="py-1"
          disabled={uploadOption !== ReadOptions.URL}
          placeholder="Enter URL to Version Definition File"
          value={fileUrl}
          onChange={(e: ChangeEvent<HTMLInputElement>) => {
            setFileUrl(e.target.value)
          }}
        />
      </Modal.Body>
      <Modal.Footer>
        <DefaultButton size="sm" onClick={handleClose}>
          CANCEL
        </DefaultButton>
        <Button
          variant="success"
          size="sm"
          onClick={readVersionInfo}
          disabled={!file && !fileUrl}
        >
          READ VERSION INFO
        </Button>
      </Modal.Footer>
    </Modal>
  )
}

export default AddVersionModal
