<template>
  <el-dialog
    :model-value="modelValue"
    title="调整头像"
    width="560px"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    @closed="handleDialogClosed"
  >
    <div class="cropper-body">
      <div class="cropper-stage">
        <div class="cropper-wrapper">
          <img
            ref="imageRef"
            :src="imageUrl"
            class="cropper-image"
            alt="avatar preview"
          />
          <div class="circle-overlay" />
        </div>
        <div class="cropper-controls">
          <el-button-group>
            <el-button @click="zoom(-0.1)" :disabled="!cropper">缩小</el-button>
            <el-button @click="zoom(0.1)" :disabled="!cropper">放大</el-button>
            <el-button @click="rotate(-15)" :disabled="!cropper"
              >左转15°</el-button
            >
            <el-button @click="rotate(15)" :disabled="!cropper"
              >右转15°</el-button
            >
            <el-button @click="reset" :disabled="!cropper">重置</el-button>
          </el-button-group>
        </div>
      </div>
      <div class="preview-section">
        <div class="preview-title">圆形预览</div>
        <div class="preview-circle">
          <canvas ref="previewCanvas" width="200" height="200" />
        </div>
        <p class="preview-tip">可拖拽调整位置，使用滚轮进行缩放</p>
      </div>
    </div>
    <template #footer>
      <el-button @click="handleCancel" :disabled="confirming">取消</el-button>
      <el-button type="primary" @click="handleConfirm" :loading="confirming"
        >确定</el-button
      >
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, watch, nextTick, onBeforeUnmount } from "vue";
import Cropper from "cropperjs";
import "cropperjs/dist/cropper.css";

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false,
  },
  imageUrl: {
    type: String,
    default: "",
  },
  fileName: {
    type: String,
    default: "avatar.png",
  },
});

const emit = defineEmits(["update:modelValue", "cancel", "confirm"]);

const imageRef = ref(null);
const cropper = ref(null);
const previewCanvas = ref(null);
const confirming = ref(false);
const closeReason = ref(null);

const EXPORT_SIZE = 512;
const PREVIEW_SIZE = 200;

const destroyCropper = () => {
  if (cropper.value) {
    cropper.value.destroy();
    cropper.value = null;
  }
};

const updatePreview = () => {
  if (!cropper.value || !previewCanvas.value) return;
  const squareCanvas = cropper.value.getCroppedCanvas({
    width: PREVIEW_SIZE,
    height: PREVIEW_SIZE,
    imageSmoothingEnabled: true,
    imageSmoothingQuality: "high",
  });
  if (!squareCanvas) return;

  const circleCanvas = getCircleCanvas(squareCanvas);
  const ctx = previewCanvas.value.getContext("2d");
  if (!ctx) return;

  previewCanvas.value.width = circleCanvas.width;
  previewCanvas.value.height = circleCanvas.height;
  ctx.clearRect(0, 0, circleCanvas.width, circleCanvas.height);
  ctx.drawImage(circleCanvas, 0, 0);
};

const initCropper = () => {
  if (!imageRef.value) return;
  destroyCropper();
  cropper.value = new Cropper(imageRef.value, {
    viewMode: 1,
    dragMode: "move",
    aspectRatio: 1,
    autoCropArea: 1,
    responsive: true,
    background: false,
    guides: false,
    highlight: false,
    movable: true,
    zoomable: true,
    rotatable: true,
    scalable: false,
    minContainerWidth: 320,
    minContainerHeight: 320,
    wheelZoomRatio: 0.1,
    crop: updatePreview,
    ready() {
      updatePreview();
    },
  });
};

const scheduleCropperInit = () => {
  if (!props.imageUrl || !props.modelValue) return;
  nextTick(() => {
    const img = imageRef.value;
    if (!img) return;
    if (img.complete) {
      initCropper();
    } else {
      const listener = () => {
        initCropper();
        img.removeEventListener("load", listener);
      };
      img.addEventListener("load", listener);
    }
  });
};

watch(
  () => props.modelValue,
  (visible) => {
    if (visible) {
      closeReason.value = null;
      scheduleCropperInit();
    } else {
      destroyCropper();
    }
  }
);

watch(
  () => props.imageUrl,
  () => {
    if (props.modelValue) {
      scheduleCropperInit();
    }
  }
);

const zoom = (ratio) => {
  cropper.value?.zoom(ratio);
};

const rotate = (degree) => {
  cropper.value?.rotate(degree);
};

const reset = () => {
  cropper.value?.reset();
};

const getCircleCanvas = (sourceCanvas) => {
  const size = Math.min(sourceCanvas.width, sourceCanvas.height);
  const canvas = document.createElement("canvas");
  canvas.width = size;
  canvas.height = size;
  const ctx = canvas.getContext("2d");
  if (!ctx) return sourceCanvas;

  ctx.clearRect(0, 0, size, size);
  ctx.save();
  ctx.beginPath();
  ctx.arc(size / 2, size / 2, size / 2, 0, Math.PI * 2, false);
  ctx.closePath();
  ctx.clip();
  ctx.drawImage(sourceCanvas, 0, 0, size, size);
  ctx.restore();
  return canvas;
};

const canvasToBlob = (canvas) =>
  new Promise((resolve, reject) => {
    canvas.toBlob((blob) => {
      if (blob) {
        resolve(blob);
      } else {
        reject(new Error("头像处理失败，请重试"));
      }
    }, "image/png");
  });

const handleConfirm = async () => {
  if (!cropper.value || confirming.value) return;
  confirming.value = true;
  try {
    const exportCanvas = cropper.value.getCroppedCanvas({
      width: EXPORT_SIZE,
      height: EXPORT_SIZE,
      imageSmoothingEnabled: true,
      imageSmoothingQuality: "high",
    });
    if (!exportCanvas) {
      throw new Error("无法生成头像图像");
    }

    const circleCanvas = getCircleCanvas(exportCanvas);
    const blob = await canvasToBlob(circleCanvas);

    const baseName = props.fileName
      ? props.fileName.replace(/\.[^/.]+$/, "")
      : "avatar";
    const file = new File([blob], `${baseName || "avatar"}.png`, {
      type: "image/png",
    });

    closeReason.value = "confirm";
    emit("confirm", file);
    emit("update:modelValue", false);
  } catch (error) {
    console.error(error);
  } finally {
    confirming.value = false;
  }
};

const handleCancel = () => {
  if (confirming.value) return;
  closeReason.value = "cancel";
  emit("update:modelValue", false);
};

const handleDialogClosed = () => {
  destroyCropper();
  if (previewCanvas.value) {
    const ctx = previewCanvas.value.getContext("2d");
    ctx?.clearRect(0, 0, previewCanvas.value.width, previewCanvas.value.height);
  }
  if (closeReason.value !== "confirm") {
    emit("cancel");
  }
  closeReason.value = null;
};

onBeforeUnmount(() => {
  destroyCropper();
});
</script>

<style scoped>
.cropper-body {
  display: flex;
  gap: 1.5rem;
  align-items: flex-start;
  flex-wrap: wrap;
}

.cropper-stage {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.cropper-wrapper {
  position: relative;
  width: 320px;
  height: 320px;
  border-radius: 1rem;
  overflow: hidden;
  background: #111;
}

.cropper-image {
  max-width: 100%;
  width: 100%;
  display: block;
}

.circle-overlay {
  position: absolute;
  inset: 0;
  pointer-events: none;
  background: radial-gradient(
    circle at center,
    rgba(0, 0, 0, 0) 48%,
    rgba(0, 0, 0, 0.65) 52%
  );
}

.cropper-controls {
  display: flex;
  justify-content: center;
}

.preview-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1rem;
}

.preview-title {
  font-weight: 600;
  color: #1f2937;
}

.preview-circle {
  width: 200px;
  height: 200px;
  border-radius: 50%;
  overflow: hidden;
  border: 2px solid #3b82f6;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f3f4f6;
}

.preview-tip {
  margin: 0;
  font-size: 0.875rem;
  color: #6b7280;
}

@media (max-width: 720px) {
  .cropper-body {
    flex-direction: column;
    align-items: center;
  }

  .cropper-wrapper {
    width: 280px;
    height: 280px;
  }
}
</style>
